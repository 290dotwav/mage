package org.mage.test.rollback;

import mage.abilities.Ability;
import mage.choices.Choice;
import mage.constants.Outcome;
import mage.constants.PhaseStep;
import mage.constants.RangeOfInfluence;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.RollbackPoints;
import mage.game.combat.CombatGroup;
import mage.game.permanent.Permanent;
import mage.target.Target;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.player.TestComputerPlayer;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestCommanderDuelBase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Going back to a rollback point while a seat is being asked something: the vote is granted from
 * another thread while the question is open (as RollbackRequests does), the seat then gives its
 * now late answer, and the game must be the kept copy, ask again from there, and never carry the
 * late answer over.
 * <p>
 * The owner's game, 5 October: K'rrik attacked, was blocked by a deathtouch Varragoth and died; the
 * table asked him whether to move his commander to the command zone (903.9a), and while it asked he
 * went back to "Combat damage". The copy was restored, K'rrik attacking, but a combat damage step
 * deals its damage before anybody gets priority (510.1-510.3): K'rrik died again at once, the same
 * question came back, and his "Move to command" put his commander in the command zone.
 */
public class RollbackQuestionTest extends CardTestCommanderDuelBase {

    /** A test player that says what it is asked, and lets a test act while a question is open. */
    public static final class AskingPlayer extends TestPlayer {

        public final List<String> asked = new ArrayList<>();
        /** Asked a yes/no question: a non-null result is the answer given. */
        public BiFunction<Game, String, Boolean> onUse;
        /** Any other question ("attackers", "target …", "choice …"), before the answer is given. */
        public BiConsumer<Game, String> onAsk;
        public Consumer<Game> onPriority;

        public AskingPlayer(TestComputerPlayer computerPlayer) {
            super(computerPlayer);
        }

        @Override
        public boolean priority(Game game) {
            if (onPriority != null) {
                onPriority.accept(game);
            }
            return super.priority(game);
        }

        @Override
        public boolean chooseUse(Outcome outcome, String message, String secondMessage, String trueText, String falseText, Ability source, Game game) {
            asked.add("use " + message.replaceAll("<[^>]*>", "").replaceAll(" \\[[0-9a-f]{3}]", ""));
            if (onUse != null) {
                Boolean answer = onUse.apply(game, message);
                if (answer != null) {
                    return answer;
                }
            }
            return super.chooseUse(outcome, message, secondMessage, trueText, falseText, source, game);
        }

        @Override
        public void selectAttackers(Game game, UUID attackingPlayerId) {
            ask(game, "attackers turn " + game.getTurnNum());
            super.selectAttackers(game, attackingPlayerId);
        }

        @Override
        public void selectBlockers(Ability source, Game game, UUID defendingPlayerId) {
            ask(game, "blockers turn " + game.getTurnNum());
            super.selectBlockers(source, game, defendingPlayerId);
        }

        @Override
        public boolean chooseTarget(Outcome outcome, Target target, Ability source, Game game) {
            ask(game, "target " + target.getMessage(game));
            return super.chooseTarget(outcome, target, source, game);
        }

        @Override
        public boolean choose(Outcome outcome, Target target, Ability source, Game game, Map<String, Serializable> options) {
            ask(game, "target " + target.getMessage(game));
            return super.choose(outcome, target, source, game, options);
        }

        @Override
        public boolean choose(Outcome outcome, Choice choice, Game game) {
            ask(game, "choice " + choice.getMessage());
            return super.choose(outcome, choice, game);
        }

        private void ask(Game game, String what) {
            if (what.contains("starting player")) {
                return; // the game's start, not a question of this test
            }
            asked.add(what);
            if (onAsk != null) {
                onAsk.accept(game, what);
            }
        }

        public long count(String prefix) {
            return asked.stream().filter(a -> a.startsWith(prefix)).count();
        }
    }

    @Override
    protected TestPlayer createNewPlayer(String playerName, RangeOfInfluence rangeOfInfluence) {
        return new AskingPlayer(new TestComputerPlayer(playerName, rangeOfInfluence));
    }

    private AskingPlayer asking(TestPlayer player) {
        return (AskingPlayer) player;
    }

    private static RollbackPoints.Point find(Game game, RollbackPoints.Kind kind, int turn, String what) {
        for (RollbackPoints.Point p : game.getRollbackPoints().list()) {
            if (p.getKind() == kind && p.getTurn() == turn && p.getLabel().contains(what)) {
                return p;
            }
        }
        Assert.fail("no " + kind + " point on turn " + turn + " for " + what);
        return null;
    }

    /** The vote's last yes arrives on another thread while the game thread is inside the question (RollbackRequests.settle). */
    private static void grantFromAnotherThread(Game game, RollbackPoints.Point point) {
        boolean[] granted = new boolean[1];
        Thread vote = new Thread(() -> granted[0] = game.rollbackToPoint(point.getId()), "CALL rollback vote");
        vote.start();
        try {
            vote.join(10_000);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        Assert.assertFalse("the vote's thread is stuck", vote.isAlive());
        Assert.assertTrue("the rollback is granted", granted[0]);
        Assert.assertTrue(game.executingRollback());
    }

    private static Permanent permanent(Game game, String name) {
        return game.getBattlefield().getAllActivePermanents().stream()
                .filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    @Test
    public void test_BackToCombatDamage_WhileTheCommanderIsAskedForTheCommandZone() {
        // the owner's game, with a Grizzly Bears commander and a deathtouch blocker
        addCard(Zone.COMMAND, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Typhoid Rats"); // 1/1 deathtouch

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        attack(3, playerA, "Grizzly Bears");
        block(3, playerB, "Typhoid Rats", "Grizzly Bears");

        AskingPlayer a = asking(playerA);
        List<String> seenAfter = new ArrayList<>();
        a.onUse = (game, message) -> {
            if (!message.contains("command zone")) {
                return null;
            }
            if (a.count("use Move Grizzly Bears") == 1) {
                Assert.assertNull("the commander died in the combat damage", permanent(game, "Grizzly Bears"));
                grantFromAnotherThread(game, find(game, RollbackPoints.Kind.STEP, 3, "Combat damage"));
                return true; // his "Move to command", too late: the question is gone
            }
            return false; // asked again after the damage dealt again: leave it in the graveyard
        };
        a.onPriority = game -> {
            if (game.getTurnNum() == 3 && game.getTurnStepType() == PhaseStep.DECLARE_BLOCKERS && a.count("use Move Grizzly Bears") == 1) {
                Permanent bears = permanent(game, "Grizzly Bears");
                Permanent rats = permanent(game, "Typhoid Rats");
                boolean blocked = false;
                for (CombatGroup group : game.getCombat().getGroups()) {
                    blocked |= bears != null && group.getAttackers().contains(bears.getId()) && rats != null && group.getBlockers().contains(rats.getId());
                }
                seenAfter.add("bears " + (bears == null ? "gone" : (bears.isAttacking() ? "attacking" : "home"))
                        + ", rats " + (rats == null ? "gone" : "there")
                        + ", " + (blocked ? "blocked" : "unblocked")
                        + ", command zone " + game.getState().getCommand().stream().filter(o -> o.getName().equals("Grizzly Bears")).count());
            }
        };

        setStopAt(3, PhaseStep.END_TURN);
        execute();

        Assert.assertEquals("asked once, then again after going back: " + a.asked, 2, a.count("use Move Grizzly Bears"));
        // back to "Combat damage" is back to the last priority before the damage: attacking, blocked, his to act
        Assert.assertFalse("a priority before the damage again: " + a.asked, seenAfter.isEmpty());
        Assert.assertEquals("bears attacking, rats there, blocked, command zone 0", seenAfter.get(0));
        // the late "Move to command" was not carried over; the second answer was
        assertCommandZoneCount(playerA, "Grizzly Bears", 0);
        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertGraveyardCount(playerB, "Typhoid Rats", 1);
        assertLife(playerB, 40);
        // the damage point went with it, and the damage step took a new one
        long damagePoints = currentGame.getRollbackPoints().list().stream()
                .filter(p -> p.getKind() == RollbackPoints.Kind.STEP && p.getTurn() == 3 && p.getStep() == PhaseStep.COMBAT_DAMAGE).count();
        Assert.assertEquals(1, damagePoints);
    }

    @Test
    public void test_BackToCombatDamage_AfterTheCombat_AttackersBackWithAPriority() {
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Typhoid Rats");

        attack(3, playerA, "Grizzly Bears");
        block(3, playerB, "Typhoid Rats", "Grizzly Bears");
        runCode("back to combat damage", 3, PhaseStep.POSTCOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertNull(permanent(game, "Grizzly Bears"));
            RollbackPoints.Point damage = find(game, RollbackPoints.Kind.STEP, 3, "Combat damage");
            Assert.assertEquals("resumed at the priority before the damage", PhaseStep.DECLARE_BLOCKERS, damage.getResumeStep());
            Assert.assertTrue(game.rollbackToPoint(damage.getId()));
        });

        AskingPlayer a = asking(playerA);
        List<String> seen = new ArrayList<>();
        List<String> trail = new ArrayList<>();
        a.onPriority = game -> {
            trail.add(game.getTurnNum() + " " + game.getTurnStepType());
            if (game.getTurnNum() == 3 && game.getTurnStepType() == PhaseStep.DECLARE_BLOCKERS) {
                Permanent bears = permanent(game, "Grizzly Bears");
                seen.add(bears != null && bears.isAttacking() && bears.isBlocked(game) ? "attacking, blocked" : "not attacking");
            }
        };

        setStopAt(3, PhaseStep.END_TURN);
        execute();

        // the declare blockers priority: once before the damage, once more after going back
        Assert.assertEquals(trail.toString(), 2, seen.size());
        Assert.assertEquals("attacking, blocked", seen.get(1));
        // the blocks were not asked again (that is going back to "Declare blockers")
        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertGraveyardCount(playerB, "Typhoid Rats", 1);
    }

    @Test
    public void test_DeclareAttackers_RollbackWhileDeclaring_AskedAgainLateDeclarationDropped() {
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        attack(3, playerA, "Grizzly Bears");

        AskingPlayer a = asking(playerA);
        a.onAsk = (game, what) -> {
            if (what.equals("attackers turn 3") && a.count("attackers turn 3") == 1) {
                grantFromAnotherThread(game, find(game, RollbackPoints.Kind.STEP, 3, "Declare attackers"));
                // and the declaration goes on: the test player's attack is declared, too late
            }
        };

        setStopAt(3, PhaseStep.END_TURN);
        execute();

        // the step is played again from its start: the declaration is asked again
        Assert.assertEquals(a.asked.toString(), 2, a.count("attackers turn 3"));
        // the late declaration did not survive: nothing attacked (the test player's attack was spent on it)
        assertLife(playerB, 40);
        assertTapped("Grizzly Bears", false);
    }

    @Test
    public void test_TargetChoice_RollbackBeforeTheCast_LateTargetDropped() {
        addCard(Zone.HAND, playerA, "Flametongue Kavu"); // {3}{R}: When it enters, it deals 4 damage to target creature.
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 4);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Flametongue Kavu");
        addTarget(playerA, "Grizzly Bears");

        AskingPlayer a = asking(playerA);
        a.onAsk = (game, what) -> {
            if (what.startsWith("target") && a.count("target") == 1) {
                Assert.assertNotNull("the Kavu is on the battlefield, its trigger asking", permanent(game, "Flametongue Kavu"));
                grantFromAnotherThread(game, find(game, RollbackPoints.Kind.CAST, 1, "Flametongue Kavu"));
            }
        };

        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        Assert.assertEquals(a.asked.toString(), 1, a.count("target"));
        assertHandCount(playerA, "Flametongue Kavu", 1);
        assertPermanentCount(playerA, "Flametongue Kavu", 0);
        assertPermanentCount(playerB, "Grizzly Bears", 1);
        assertTappedCount("Mountain", false, 4);
    }

    @Test
    public void test_ChooseAColor_RollbackBeforeTheCast_LateColourDropped() {
        addCard(Zone.HAND, playerA, "Painter's Servant"); // {2}: As it enters, choose a color.
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Painter's Servant");
        setChoice(playerA, "Red");

        AskingPlayer a = asking(playerA);
        a.onAsk = (game, what) -> {
            if (what.startsWith("choice") && a.count("choice") == 1) {
                grantFromAnotherThread(game, find(game, RollbackPoints.Kind.CAST, 1, "Painter's Servant"));
            }
        };

        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        Assert.assertEquals(a.asked.toString(), 1, a.count("choice"));
        assertHandCount(playerA, "Painter's Servant", 1);
        assertPermanentCount(playerA, "Painter's Servant", 0);
        assertTappedCount("Mountain", false, 2);
    }

    @Test
    public void test_RollbackWhileTheOtherSeatDecidesItsBlocks_ItsBlocksAreNotCarriedOver() {
        // B, a machine seat, is choosing its blocks when the table agrees to go back to A's main phase
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Typhoid Rats");

        attack(3, playerA, "Grizzly Bears");
        block(3, playerB, "Typhoid Rats", "Grizzly Bears");

        AskingPlayer b = asking(playerB);
        b.onAsk = (game, what) -> {
            if (what.equals("blockers turn 3") && b.count("blockers turn 3") == 1) {
                grantFromAnotherThread(game, find(game, RollbackPoints.Kind.STEP, 3, "Main 1"));
                // and B's block is declared, too late
            }
        };

        setStopAt(3, PhaseStep.END_TURN);
        execute();

        Assert.assertEquals(b.asked.toString(), 1, b.count("blockers turn 3"));
        // back in main 1, the attack (spent by the test player) was declared again with nobody:
        // no block, no damage, both creatures alive
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerB, "Typhoid Rats", 1);
        assertTapped("Grizzly Bears", false);
        assertLife(playerB, 40);
    }
}
