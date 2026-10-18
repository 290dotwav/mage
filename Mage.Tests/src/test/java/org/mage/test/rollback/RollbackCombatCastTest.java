package org.mage.test.rollback;

import mage.constants.PhaseStep;
import mage.constants.RangeOfInfluence;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.RollbackPoints;
import mage.game.combat.CombatGroup;
import mage.game.permanent.Permanent;
import mage.view.GameView;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.player.TestComputerPlayer;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestCommander4Players;

import java.util.ArrayList;
import java.util.List;

/**
 * Going back to before an instant cast in the middle of a combat, at a four-seat table.
 * <p>
 * The owner's game, 18 October: Francis attacked fgmuller (10 life) with a 13/13 Orc Army made
 * unblockable; in the declare blockers step Miaou sacrificed three Treasures and cast Crackling Doom,
 * which killed Francis. The table went back "before Miaou casts Crackling Doom", and the game must be
 * that moment again: the same step, the attackers attacking and the blocks as declared, the life
 * totals of then, the instant back in her hand, and her priority, the one she cast it from.
 * <p>
 * Seats here: A (Francis, active) -> D (fgmuller, attacked) -> C (Miaou, casts) -> B (Bendalf).
 */
public class RollbackCombatCastTest extends CardTestCommander4Players {

    @Override
    protected TestPlayer createNewPlayer(String playerName, RangeOfInfluence rangeOfInfluence) {
        return new RollbackQuestionTest.AskingPlayer(new TestComputerPlayer(playerName, rangeOfInfluence));
    }

    private static RollbackPoints.Point castOf(Game game, String card) {
        for (RollbackPoints.Point p : game.getRollbackPoints().list()) {
            if (p.getKind() == RollbackPoints.Kind.CAST && p.getLabel().contains(card)) {
                return p;
            }
        }
        Assert.fail("no cast point for " + card);
        return null;
    }

    private static Permanent permanent(Game game, String name) {
        return game.getBattlefield().getAllActivePermanents().stream()
                .filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    /** What C sees when she gets a priority in A's declare blockers step. */
    private String combatSeen(Game game) {
        Permanent giant = permanent(game, "Hill Giant");
        Permanent bears = permanent(game, "Grizzly Bears");
        Permanent wall = permanent(game, "Wall of Stone");
        boolean wallBlocksGiant = false;
        boolean bearsUnblocked = false;
        for (CombatGroup group : game.getCombat().getGroups()) {
            if (giant != null && group.getAttackers().contains(giant.getId()) && wall != null && group.getBlockers().contains(wall.getId())) {
                wallBlocksGiant = true;
            }
            if (bears != null && group.getAttackers().contains(bears.getId()) && group.getBlockers().isEmpty()) {
                bearsUnblocked = true;
            }
        }
        return "giant " + (giant == null ? "gone" : giant.isAttacking() ? "attacking" : "home")
                + (wallBlocksGiant ? " blocked by the wall" : " not blocked")
                + ", bears " + (bears == null ? "gone" : bears.isAttacking() ? "attacking" : "home")
                + (bearsUnblocked ? " unblocked" : "")
                + ", life A " + game.getPlayer(playerA.getId()).getLife()
                + " D " + game.getPlayer(playerD.getId()).getLife()
                + " B " + game.getPlayer(playerB.getId()).getLife()
                + ", doom in hand " + game.getPlayer(playerC.getId()).getHand().getCards(game).stream()
                .filter(c -> c.getName().equals("Crackling Doom")).count();
    }

    @Test
    public void test_BackBeforeAnInstantInDeclareBlockers_SameStepSameCombatHerPriority() {
        // A attacks D with two creatures, one blocked by D's wall
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant"); // 3/3
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears"); // 2/2
        addCard(Zone.BATTLEFIELD, playerD, "Wall of Stone"); // 0/8 defender
        // C casts Crackling Doom in the declare blockers step: 2 damage to each opponent, each sacrifices its greatest power creature
        addCard(Zone.HAND, playerC, "Crackling Doom");
        addCard(Zone.BATTLEFIELD, playerC, "Mountain");
        addCard(Zone.BATTLEFIELD, playerC, "Plains");
        addCard(Zone.BATTLEFIELD, playerC, "Swamp");

        attack(5, playerA, "Hill Giant", playerD);
        attack(5, playerA, "Grizzly Bears", playerD);
        block(5, playerD, "Wall of Stone", "Hill Giant");
        castSpell(5, PhaseStep.DECLARE_BLOCKERS, playerC, "Crackling Doom");

        RollbackQuestionTest.AskingPlayer c = (RollbackQuestionTest.AskingPlayer) playerC;
        List<String> seen = new ArrayList<>();
        List<String> trail = new ArrayList<>();
        int[] rewinds = new int[1];
        c.onPriority = game -> {
            if (game.getTurnNum() == 5) {
                trail.add(rewinds[0] + " " + game.getTurnStepType() + " stack " + game.getStack().size());
                if (game.getTurnStepType() == PhaseStep.DECLARE_BLOCKERS && game.getStack().isEmpty()) {
                    seen.add(rewinds[0] + ": " + combatSeen(game) + ", views say " + new GameView(game.getState(), game, playerC.getId(), null).getRollbacks());
                }
            }
        };
        // after the Doom resolved, still in the declare blockers step, the table goes back before it
        waitStackResolved(5, PhaseStep.DECLARE_BLOCKERS, playerB);
        runCode("back before the Doom", 5, PhaseStep.DECLARE_BLOCKERS, playerB, (info, player, game) -> {
            if (rewinds[0] > 0) {
                return;
            }
            Assert.assertEquals("the Doom dealt 2 to A", 18, game.getPlayer(playerA.getId()).getLife());
            Assert.assertNull("A sacrificed his giant", permanent(game, "Hill Giant"));
            rewinds[0]++;
            Assert.assertTrue(game.rollbackToPoint(castOf(game, "Crackling Doom").getId()));
        });

        setStopAt(5, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        Assert.assertEquals("gone back once: " + trail, 1, rewinds[0]);
        // her first priority after going back: the same moment as when she cast it
        String back = seen.stream().filter(s -> s.startsWith("1:")).findFirst().orElse("never: " + trail);
        Assert.assertEquals("1: giant attacking blocked by the wall, bears attacking unblocked, life A 20 D 20 B 20, doom in hand 1, views say 1", back);
        Assert.assertEquals("after going back, her first priority is in the declare blockers step: " + trail,
                "1 Declare Blockers stack 0", trail.stream().filter(s -> s.startsWith("1 ")).findFirst().orElse("none"));
        // the test player's cast was spent: she passes, and the combat is the one declared
        assertLife(playerA, 20);
        assertLife(playerB, 20);
        assertLife(playerD, 20 - 2);
        assertPermanentCount(playerA, "Hill Giant", 1);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerD, "Wall of Stone", 1);
        assertHandCount(playerC, "Crackling Doom", 1);
        // every view after it says the game went back once: a screen tells the restored game's
        // questions from the gone ones by it, whatever order they reach it in
        Assert.assertEquals(1, currentGame.getRollbackCount());
    }

    @Test
    public void test_TheDoomKilledTheActivePlayer_BackBeforeIt_HeIsBackAndOnlyTheCasterIsAsked() {
        // the owner's game, seat for seat: the caster is the last to get priority in A's turn (Miaou),
        // and the Doom kills the active player (Francis at 2 life)
        setLife(playerA, 2);
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerD, "Wall of Stone");
        addCard(Zone.HAND, playerB, "Crackling Doom");
        addCard(Zone.BATTLEFIELD, playerB, "Mountain");
        addCard(Zone.BATTLEFIELD, playerB, "Plains");
        addCard(Zone.BATTLEFIELD, playerB, "Swamp");

        attack(5, playerA, "Hill Giant", playerD);
        block(5, playerD, "Wall of Stone", "Hill Giant");
        castSpell(5, PhaseStep.DECLARE_BLOCKERS, playerB, "Crackling Doom");

        int[] rewinds = new int[1];
        List<String> trail = new ArrayList<>();
        for (TestPlayer seat : new TestPlayer[]{playerA, playerB, playerC, playerD}) {
            ((RollbackQuestionTest.AskingPlayer) seat).onPriority = game -> {
                if (game.getTurnNum() != 5 || rewinds[0] == 0 || game.getTurnStepType() != PhaseStep.DECLARE_BLOCKERS) {
                    return;
                }
                String doom = seat == playerB ? (game.getPlayer(playerB.getId()).getPlayable(game, true).stream()
                        .anyMatch(a -> a.getSourceId() != null && game.getCard(a.getSourceId()) != null
                                && game.getCard(a.getSourceId()).getName().equals("Crackling Doom")) ? " can cast the Doom" : " cannot cast the Doom") : "";
                trail.add(seat.getName() + doom);
            };
        }
        // A is out: his turn goes on without him (800.4a), and D asks to go back in its second main phase
        runCode("back before the Doom", 5, PhaseStep.POSTCOMBAT_MAIN, playerD, (info, player, game) -> {
            if (rewinds[0] > 0) {
                return;
            }
            Assert.assertTrue("the Doom killed A", game.getPlayer(playerA.getId()).hasLost());
            rewinds[0]++;
            Assert.assertTrue(game.rollbackToPoint(castOf(game, "Crackling Doom").getId()));
        });

        setStopAt(5, PhaseStep.END_TURN);
        execute();

        Assert.assertEquals(1, rewinds[0]);
        Assert.assertFalse("A is back", currentGame.getPlayer(playerA.getId()).hasLost());
        // A, D and C had passed before she cast: going back gives the priority to her, and to nobody
        // else before the damage once she passes (117.4)
        Assert.assertEquals("PlayerB can cast the Doom", String.join(", ", trail));
        assertLife(playerA, 2);
        assertLife(playerD, 20); // the giant was blocked
        assertPermanentCount(playerA, "Hill Giant", 1);
        assertHandCount(playerB, "Crackling Doom", 1);
    }
}
