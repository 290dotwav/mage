package org.mage.test.rollback;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.RollbackPoints;
import mage.server.game.RollbackVote;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rollback points (steps, casts, lands) and going back to one of them; the vote that decides it.
 */
public class RollbackPointsTest extends CardTestPlayerBase {

    private static RollbackPoints.Point find(Game game, RollbackPoints.Kind kind, int turn, String what) {
        for (RollbackPoints.Point p : game.getRollbackPoints().list()) {
            if (p.getKind() == kind && p.getTurn() == turn && (what == null || p.getLabel().contains(what))) {
                return p;
            }
        }
        Assert.fail("no " + kind + " point on turn " + turn + (what == null ? "" : " for " + what) + " in " + labels(game));
        return null;
    }

    private static List<String> labels(Game game) {
        List<String> out = new ArrayList<>();
        for (RollbackPoints.Point p : game.getRollbackPoints().list()) {
            out.add(p.getKind() + " " + p.getLabel());
        }
        return out;
    }

    @Test
    public void test_PointsAtStepStartBeforeCastAndBeforeLand() {
        addCard(Zone.HAND, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Lightning Bolt");

        playLand(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Mountain");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerB, 17);
        RollbackPoints.Point main1 = find(currentGame, RollbackPoints.Kind.STEP, 1, "Main 1");
        RollbackPoints.Point land = find(currentGame, RollbackPoints.Kind.LAND, 1, "plays Mountain");
        RollbackPoints.Point cast = find(currentGame, RollbackPoints.Kind.CAST, 1, "casts Lightning Bolt");
        Assert.assertEquals("Turn 1 PlayerA — Main 1", main1.getLabel());
        Assert.assertEquals("before PlayerA casts Lightning Bolt", cast.getTarget());
        Assert.assertEquals(playerA.getId(), cast.getPlayerId());
        Assert.assertEquals(PhaseStep.PRECOMBAT_MAIN, cast.getStep());
        Assert.assertTrue("chronological: step, then land, then cast", main1.getId() < land.getId() && land.getId() < cast.getId());
        find(currentGame, RollbackPoints.Kind.STEP, 1, "Upkeep");
        find(currentGame, RollbackPoints.Kind.STEP, 1, "Declare attackers");
        // a mana ability and the resolution leave no point of their own
        long casts = currentGame.getRollbackPoints().list().stream().filter(p -> p.getKind() == RollbackPoints.Kind.CAST).count();
        Assert.assertEquals(1, casts);
    }

    @Test
    public void test_BackBeforeCast_CardInHandLandUntapped() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA);
        runCode("back before the bolt", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertEquals(17, game.getPlayer(playerB.getId()).getLife());
            Assert.assertTrue(game.rollbackToPoint(find(game, RollbackPoints.Kind.CAST, 1, "Lightning Bolt").getId()));
        });

        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerB, 20);
        assertHandCount(playerA, "Lightning Bolt", 1);
        assertGraveyardCount(playerA, "Lightning Bolt", 0);
        assertTapped("Mountain", false);
        // the point gone back to goes too: casting again would take it again
        long casts = currentGame.getRollbackPoints().list().stream().filter(p -> p.getKind() == RollbackPoints.Kind.CAST).count();
        Assert.assertEquals(0, casts);
        find(currentGame, RollbackPoints.Kind.STEP, 1, "Main 2");
    }

    @Test
    public void test_BackBeforeLand_LandInHandDropAgain() {
        addCard(Zone.HAND, playerA, "Mountain");

        playLand(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Mountain");
        runCode("back before the land", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertEquals(1, game.getBattlefield().getAllActivePermanents(playerA.getId()).size());
            Assert.assertTrue(game.rollbackToPoint(find(game, RollbackPoints.Kind.LAND, 1, "Mountain").getId()));
        });
        runCode("the land drop is there again", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertTrue(game.getPlayer(playerA.getId()).canPlayLand());
        });

        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Mountain", 1);
        assertPermanentCount(playerA, "Mountain", 0);
    }

    @Test
    public void test_BackToUpkeep_StepPlayedAgainFromItsStart() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Lightning Bolt");

        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        runCode("back to the upkeep", 3, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            // the card drawn this turn, the bolt gone
            Assert.assertEquals(1, game.getPlayer(playerA.getId()).getHand().size());
            RollbackPoints.Point upkeep = find(game, RollbackPoints.Kind.STEP, 3, "Upkeep");
            Assert.assertEquals("Turn 3 PlayerA — Upkeep", upkeep.getLabel());
            Assert.assertTrue(game.rollbackToPoint(upkeep.getId()));
        });

        setStopAt(3, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerB, 20);
        assertHandCount(playerA, "Lightning Bolt", 1);
        // the draw step was played again from the upkeep: one card drawn, not two
        assertHandCount(playerA, 2);
        assertTapped("Mountain", false);
        // the upkeep point stays (the game resumed at it), only one of it
        long upkeeps = currentGame.getRollbackPoints().list().stream()
                .filter(p -> p.getKind() == RollbackPoints.Kind.STEP && p.getTurn() == 3 && p.getStep() == PhaseStep.UPKEEP).count();
        Assert.assertEquals(1, upkeeps);
    }

    @Test
    public void test_BackToDeclareAttackers_AttackDeclaredAgain() {
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        attack(3, playerA, "Grizzly Bears");
        runCode("back to the attack", 3, PhaseStep.POSTCOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertEquals(18, game.getPlayer(playerB.getId()).getLife());
            Assert.assertTrue(game.rollbackToPoint(find(game, RollbackPoints.Kind.STEP, 3, "Declare attackers").getId()));
        });

        setStopAt(3, PhaseStep.END_TURN);
        execute();

        // the test player's attack was spent: declared again with nobody, nothing was dealt
        assertLife(playerB, 20);
        assertTapped("Grizzly Bears", false);
    }

    @Test
    public void test_WindowIsTheTurnAndOneRound_OlderTurnsByTheirStart() {
        setStopAt(6, PhaseStep.PRECOMBAT_MAIN);
        execute();

        List<RollbackPoints.Point> list = currentGame.getRollbackPoints().list();
        int oldestFine = Integer.MAX_VALUE;
        List<Integer> turnStarts = new ArrayList<>();
        for (RollbackPoints.Point p : list) {
            if (p.getKind() == RollbackPoints.Kind.TURN) {
                turnStarts.add(p.getTurn());
            } else {
                oldestFine = Math.min(oldestFine, p.getTurn());
            }
        }
        // two players: turn 6 and the two before it (one round), then their own turn starts (four turns)
        Assert.assertEquals(labels(currentGame).toString(), 4, oldestFine);
        Assert.assertEquals(labels(currentGame).toString(), 1, turnStarts.size());
        Assert.assertEquals(3, (int) turnStarts.get(0));
        Assert.assertTrue(currentGame.getRollbackPoints().countWithState() <= RollbackPoints.MAX_POINTS);
    }

    @Test
    public void test_BackToAnOlderTurnStart() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Lightning Bolt");

        castSpell(5, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        waitStackResolved(5, PhaseStep.PRECOMBAT_MAIN, playerA);
        runCode("back to turn 2", 5, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertTrue(game.rollbackToPoint(find(game, RollbackPoints.Kind.TURN, 2, null).getId()));
        });

        setStopAt(5, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        // turns 2 to 5 played again; the bolt (spent by the test player) stays in hand
        assertLife(playerB, 20);
        assertHandCount(playerA, "Lightning Bolt", 1);
    }

    private RollbackPoints.Point anyPoint() {
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();
        return currentGame.getRollbackPoints().list().get(0);
    }

    @Test
    public void test_Vote_HumansDecide_AisSayYes() {
        RollbackPoints.Point point = anyPoint();
        UUID ann = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID bot = UUID.randomUUID();
        List<RollbackVote.Seat> seats = new ArrayList<>();
        seats.add(new RollbackVote.Seat(ann, "Ann", true));
        seats.add(new RollbackVote.Seat(bob, "Bob", true));
        seats.add(new RollbackVote.Seat(bot, "Bot", false));

        RollbackVote vote = new RollbackVote(point, bob, "Bob", seats, 0L);
        Assert.assertEquals(RollbackVote.Answer.YES, vote.answerOf(bob)); // the asker
        Assert.assertEquals(RollbackVote.Answer.YES, vote.answerOf(bot)); // an AI
        Assert.assertEquals(RollbackVote.Answer.PENDING, vote.answerOf(ann));
        Assert.assertFalse(vote.settleIfUnanimous());
        Assert.assertFalse("the asker has nothing to answer", vote.answer(bob, false));
        Assert.assertFalse("an AI has nothing to answer", vote.answer(bot, false));
        Assert.assertTrue(vote.isOpen());
        Assert.assertTrue(vote.answer(ann, true));
        Assert.assertEquals(RollbackVote.Outcome.ACCEPTED, vote.getOutcome());
        Assert.assertEquals(RollbackVote.TIMEOUT_MILLIS, vote.getDeadline());
    }

    @Test
    public void test_Vote_OneRefusalCancels() {
        RollbackPoints.Point point = anyPoint();
        UUID ann = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        List<RollbackVote.Seat> seats = new ArrayList<>();
        seats.add(new RollbackVote.Seat(ann, "Ann", true));
        seats.add(new RollbackVote.Seat(bob, "Bob", true));
        seats.add(new RollbackVote.Seat(cid, "Cid", true));

        RollbackVote vote = new RollbackVote(point, ann, "Ann", seats, 0L);
        Assert.assertTrue(vote.answer(bob, true));
        Assert.assertTrue(vote.isOpen());
        Assert.assertTrue(vote.answer(cid, false));
        Assert.assertEquals(RollbackVote.Outcome.REFUSED, vote.getOutcome());
        Assert.assertEquals("Cid", vote.getRefusedBy());
        Assert.assertFalse("over", vote.answer(bob, true));
        Assert.assertFalse(vote.timeOut());
    }

    @Test
    public void test_Vote_SilenceIsARefusal_AloneIsAccepted() {
        RollbackPoints.Point point = anyPoint();
        UUID ann = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID bot = UUID.randomUUID();
        List<RollbackVote.Seat> seats = new ArrayList<>();
        seats.add(new RollbackVote.Seat(ann, "Ann", true));
        seats.add(new RollbackVote.Seat(bob, "Bob", true));

        RollbackVote vote = new RollbackVote(point, ann, "Ann", seats, 0L);
        Assert.assertTrue(vote.timeOut());
        Assert.assertEquals(RollbackVote.Outcome.TIMEOUT, vote.getOutcome());
        Assert.assertEquals("Bob", vote.getRefusedBy());
        Assert.assertEquals(RollbackVote.Answer.NO, vote.answerOf(bob));

        List<RollbackVote.Seat> withBots = new ArrayList<>();
        withBots.add(new RollbackVote.Seat(ann, "Ann", true));
        withBots.add(new RollbackVote.Seat(bot, "Bot", false));
        RollbackVote alone = new RollbackVote(point, ann, "Ann", withBots, 0L);
        Assert.assertTrue("nobody human but the asker: accepted at once", alone.settleIfUnanimous());
        Assert.assertEquals(RollbackVote.Outcome.ACCEPTED, alone.getOutcome());
    }

    @Test
    public void test_Vote_DeadDoNotVote_AndGoingOutMidVoteIsAYes() {
        RollbackPoints.Point point = anyPoint();
        UUID ann = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        UUID dan = UUID.randomUUID();
        List<RollbackVote.Seat> seats = new ArrayList<>();
        seats.add(new RollbackVote.Seat(ann, "Ann", true));
        seats.add(new RollbackVote.Seat(bob, "Bob", true));
        seats.add(new RollbackVote.Seat(cid, "Cid", true));
        seats.add(new RollbackVote.Seat(dan, "Dan", true, true)); // dead: no vote, yes

        RollbackVote vote = new RollbackVote(point, ann, "Ann", seats, 0L);
        Assert.assertEquals(RollbackVote.Answer.YES, vote.answerOf(dan));
        Assert.assertFalse("the dead have nothing to answer", vote.answer(dan, false));
        Assert.assertTrue(vote.answer(bob, true));
        Assert.assertTrue("Cid is still to answer", vote.isOpen());
        Assert.assertTrue("Cid concedes", vote.out(cid));
        Assert.assertEquals(RollbackVote.Answer.YES, vote.answerOf(cid));
        Assert.assertEquals(RollbackVote.Outcome.ACCEPTED, vote.getOutcome());
        Assert.assertTrue(vote.claimSettle());
        Assert.assertFalse("settled once", vote.claimSettle());
        Assert.assertFalse("over", vote.out(bob));

        List<RollbackVote.Seat> living = new ArrayList<>();
        living.add(new RollbackVote.Seat(ann, "Ann", true));
        living.add(new RollbackVote.Seat(bob, "Bob", true));
        living.add(new RollbackVote.Seat(cid, "Cid", true));
        RollbackVote silent = new RollbackVote(point, ann, "Ann", living, 0L);
        Assert.assertTrue("Cid concedes, Bob still to answer", silent.out(cid));
        Assert.assertTrue(silent.isOpen());
        Assert.assertTrue(silent.timeOut());
        Assert.assertEquals("only the living are silent", "Bob", silent.getRefusedBy());
    }
}
