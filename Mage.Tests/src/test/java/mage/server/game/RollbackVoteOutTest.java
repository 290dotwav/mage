package mage.server.game;

import mage.constants.PhaseStep;
import mage.game.RollbackPoints;
import mage.players.Player;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestMultiPlayerBaseWithRangeAll;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * « Les joueurs morts ne peuvent pas voter, c'est oui d'office »: a seat out of the game (lost,
 * conceded, left) does not vote on a rollback, at the opening of the vote or while it is open,
 * and the vote does not wait on it. The four seats are played as people here (their test players
 * are machines, which say yes by themselves).
 */
public class RollbackVoteOutTest extends CardTestMultiPlayerBaseWithRangeAll {

    private ScheduledExecutorService timer;
    private final AtomicReference<RollbackVote> last = new AtomicReference<>();
    private final Map<UUID, UUID> playerOfUser = new HashMap<>();
    private final Map<UUID, UUID> userOfPlayer = new HashMap<>();

    @Before
    public void setUpVote() {
        timer = Executors.newSingleThreadScheduledExecutor();
        RollbackVote.setListener((gameId, vote, users) -> last.set(vote));
    }

    @After
    public void tearDownVote() {
        RollbackVote.setListener(null);
        timer.shutdownNow();
    }

    /** Every seat a person, every person at the table; the users are made up. */
    private RollbackRequests requests() {
        for (Player p : currentGame.getState().getPlayers().values()) {
            UUID user = UUID.randomUUID();
            playerOfUser.put(user, p.getId());
            userOfPlayer.put(p.getId(), user);
        }
        return new RollbackRequests(currentGame, playerOfUser::get, id -> Optional.empty(),
                p -> true, id -> true, HashSet::new, timer);
    }

    private RollbackPoints.Point anyPoint() {
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();
        return currentGame.getRollbackPoints().list().get(0);
    }

    @Test
    public void test_DeadSeatSaysYes_VotePassesWithoutIt() {
        RollbackPoints.Point point = anyPoint();
        RollbackRequests requests = requests();
        Player d = currentGame.getPlayer(playerD.getId());
        d.lost(currentGame); // dead before anybody asks
        Assert.assertFalse(d.isInGame());

        requests.request(userOfPlayer.get(playerA.getId()), point.getId());
        RollbackVote vote = requests.current();
        Assert.assertNotNull("B and C are still to answer", vote);
        Assert.assertEquals(RollbackVote.Answer.YES, vote.answerOf(playerD.getId()));
        Assert.assertEquals(RollbackVote.Answer.PENDING, vote.answerOf(playerB.getId()));
        Assert.assertFalse("the dead do not vote", vote.isPending(playerD.getId()));
        Assert.assertFalse("the dead have nothing to answer", requests.answer(userOfPlayer.get(playerD.getId()), false));
        boolean dShown = false;
        for (RollbackVote.Seat seat : vote.getSeats()) {
            if (seat.getPlayerId().equals(playerD.getId())) {
                dShown = true;
                Assert.assertTrue(seat.isOut());
                Assert.assertTrue("still a person: shown out, not as an AI", seat.isHuman());
            }
        }
        Assert.assertTrue("the dead seat is shown, with its yes", dShown);

        Assert.assertTrue(requests.answer(userOfPlayer.get(playerB.getId()), true));
        Assert.assertTrue(vote.isOpen());
        Assert.assertTrue(requests.answer(userOfPlayer.get(playerC.getId()), true));
        Assert.assertEquals("the living said yes: accepted without the dead",
                RollbackVote.Outcome.ACCEPTED, vote.getOutcome());
        Assert.assertNull(requests.current());
    }

    @Test
    public void test_ConcedeMidVote_PendingBecomesYes_VoteCompletes() {
        RollbackPoints.Point point = anyPoint();
        RollbackRequests requests = requests();

        requests.request(userOfPlayer.get(playerA.getId()), point.getId());
        RollbackVote vote = requests.current();
        Assert.assertNotNull(vote);
        Assert.assertTrue(requests.answer(userOfPlayer.get(playerB.getId()), true));
        Assert.assertTrue(requests.answer(userOfPlayer.get(playerD.getId()), true));
        Assert.assertTrue("C is still to answer", vote.isOpen());

        // C concedes (their concession ends in the seat losing) while the vote waits on it
        currentGame.getPlayer(playerC.getId()).lost(currentGame);
        requests.recheck();
        Assert.assertEquals(RollbackVote.Answer.YES, vote.answerOf(playerC.getId()));
        Assert.assertEquals("nobody left to answer: accepted", RollbackVote.Outcome.ACCEPTED, vote.getOutcome());
        Assert.assertSame("the table was told", vote, last.get());
        Assert.assertNull(requests.current());
    }

    @Test
    public void test_LeaveMidVote_OthersStillDecide() {
        RollbackPoints.Point point = anyPoint();
        RollbackRequests requests = requests();

        requests.request(userOfPlayer.get(playerA.getId()), point.getId());
        RollbackVote vote = requests.current();
        Assert.assertNotNull(vote);

        // B leaves the table: its pending vote becomes yes, C and D still decide
        currentGame.getPlayer(playerB.getId()).leave();
        requests.recheck();
        Assert.assertEquals(RollbackVote.Answer.YES, vote.answerOf(playerB.getId()));
        Assert.assertTrue(vote.isOpen());
        Assert.assertSame("the change was pushed", vote, last.get());

        Assert.assertTrue(requests.answer(userOfPlayer.get(playerC.getId()), true));
        Assert.assertTrue(requests.answer(userOfPlayer.get(playerD.getId()), false));
        Assert.assertEquals(RollbackVote.Outcome.REFUSED, vote.getOutcome());
        Assert.assertEquals("PlayerD", vote.getRefusedBy());
    }

    @Test
    public void test_OutMidVote_ByTheClock() throws InterruptedException {
        RollbackPoints.Point point = anyPoint();
        RollbackRequests requests = requests();

        requests.request(userOfPlayer.get(playerA.getId()), point.getId());
        RollbackVote vote = requests.current();
        Assert.assertNotNull(vote);
        Assert.assertTrue(requests.answer(userOfPlayer.get(playerB.getId()), true));
        Assert.assertTrue(requests.answer(userOfPlayer.get(playerC.getId()), true));

        // nobody calls recheck: the vote's own watch finds D gone within a second or so
        currentGame.getPlayer(playerD.getId()).lost(currentGame);
        long until = System.currentTimeMillis() + 5_000;
        while (vote.isOpen() && System.currentTimeMillis() < until) {
            Thread.sleep(50);
        }
        Assert.assertNotEquals("closed by the watch, not by the 30 s", null, vote.getOutcome());
        Assert.assertNotEquals(RollbackVote.Outcome.TIMEOUT, vote.getOutcome());
        Assert.assertNotEquals(RollbackVote.Outcome.REFUSED, vote.getOutcome());
    }
}
