package org.mage.test.rollback;

import mage.cards.decks.Deck;
import mage.choices.ChoiceColor;
import mage.constants.MultiplayerAttackOption;
import mage.constants.Outcome;
import mage.constants.RangeOfInfluence;
import mage.game.Game;
import mage.game.TwoPlayerDuel;
import mage.game.mulligan.MulliganType;
import mage.player.human.HumanPlayer;
import mage.util.ThreadUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * A person's seat (their HumanPlayer, a browser or a Claude chair behind it) when a rollback is
 * granted while it is being asked: the game aborts it (Game.rollbackToPoint), the question ends at
 * once, and an answer sent for it afterwards - the click that was on its way, the chair's late
 * reply - is dropped instead of answering the next question the restored game asks.
 * <p>
 * The game thread and the network's call thread are real threads here, as on the server.
 */
public class RollbackHumanQuestionTest {

    private Game game;
    private HumanPlayer human;
    /** One permit per question the game thread opens (fireAskPlayerEvent, fireChooseChoiceEvent). */
    private final Semaphore asked = new Semaphore(0);

    @Before
    public void setUp() {
        game = new TwoPlayerDuel(MultiplayerAttackOption.LEFT, RangeOfInfluence.ALL, MulliganType.GAME_DEFAULT.getMulligan(0), 60, 20, 7);
        human = new HumanPlayer("Human", RangeOfInfluence.ALL, 0);
        game.addPlayer(human, new Deck());
        game.addPlayer(new HumanPlayer("Other", RangeOfInfluence.ALL, 0), new Deck());
        game.addPlayerQueryEventListener(event -> asked.release());
    }

    /**
     * Runs two questions in a row on a game thread, as the game asks them: the first; then, released,
     * the restore (the seat's abort reset); then, released again, the second question.
     */
    private final class GameThread<T> extends Thread {
        final Function<Integer, T> question;
        final AtomicReference<T> first = new AtomicReference<>();
        final AtomicReference<T> second = new AtomicReference<>();
        final CountDownLatch firstDone = new CountDownLatch(1);
        final CountDownLatch restore = new CountDownLatch(1);
        final CountDownLatch restored = new CountDownLatch(1);
        final CountDownLatch goOn = new CountDownLatch(1);
        volatile long firstEndedAt;

        GameThread(Function<Integer, T> question) {
            super(ThreadUtils.THREAD_PREFIX_GAME + " rollback question");
            this.question = question;
            setDaemon(true);
        }

        @Override
        public void run() {
            first.set(question.apply(1));
            firstEndedAt = System.nanoTime();
            firstDone.countDown();
            try {
                restore.await(20, TimeUnit.SECONDS);
                human.abortReset(); // the restore (GameImpl.restoreRollbackPoint)
                restored.countDown();
                goOn.await(20, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
            }
            second.set(question.apply(2));
        }
    }

    /** An answer from the network, on a call thread of its own; done when their server has taken or dropped it. */
    private Thread answer(Runnable send) {
        Thread call = new Thread(send, "CALL answer");
        call.setDaemon(true);
        call.start();
        return call;
    }

    private void awaitAsked() throws InterruptedException {
        Assert.assertTrue("the game asked", asked.tryAcquire(10, TimeUnit.SECONDS));
    }

    private <T> void abortedThenAskedAgain(GameThread<T> thread, Runnable late, Runnable fresh) throws Exception {
        thread.start();
        awaitAsked();

        // the vote is granted: the seat is aborted (GameImpl.rollbackToPoint)
        long abortedAt = System.nanoTime();
        human.abort();
        Assert.assertTrue("the question ended", thread.firstDone.await(5, TimeUnit.SECONDS));
        long tookMs = (thread.firstEndedAt - abortedAt) / 1_000_000;
        Assert.assertTrue("the question ended at once, not after " + tookMs + " ms", tookMs < 1_000);

        // the game is restored; the answer to the question that was open arrives now, before the
        // restored game asks anything (the click on its way, the chair's late reply)
        thread.restore.countDown();
        Assert.assertTrue(thread.restored.await(5, TimeUnit.SECONDS));
        Thread lateCall = answer(late);
        lateCall.join(1_000);
        Assert.assertFalse("a late answer is dropped, not kept for the next question", lateCall.isAlive());

        // the restored game asks again; the seat answers that question
        thread.goOn.countDown();
        awaitAsked();
        Thread freshCall = answer(fresh);
        thread.join(10_000);
        Assert.assertFalse("the second question was answered", thread.isAlive());
        freshCall.join(5_000);
    }

    @Test
    public void test_YesNo_AbortedThenLateYesDropped() throws Exception {
        GameThread<Boolean> thread = new GameThread<>(n -> human.chooseUse(Outcome.Benefit,
                "Move K'rrik, Son of Yawgmoth to the command zone or leave it in current zone (GRAVEYARD)?", null, game));
        abortedThenAskedAgain(thread, () -> human.setResponseBoolean(true), () -> human.setResponseBoolean(false));

        Assert.assertEquals("the aborted question gives no answer", Boolean.FALSE, thread.first.get());
        Assert.assertEquals("the question asked again gets its own answer, not the late one", Boolean.FALSE, thread.second.get());
    }

    @Test
    public void test_ChooseAColor_AbortedThenLateColourDropped() throws Exception {
        GameThread<String> thread = new GameThread<>(n -> {
            ChoiceColor color = new ChoiceColor();
            return human.choose(Outcome.Benefit, color, game) ? color.getChoice() : null;
        });
        abortedThenAskedAgain(thread, () -> human.setResponseString("Red"), () -> human.setResponseString("Green"));

        Assert.assertNull("the aborted question gives no colour", thread.first.get());
        Assert.assertEquals("Green", thread.second.get());
    }

    @Test
    public void test_AnAnswerNotAfterAnAbortIsStillKeptForTheNextQuestion() throws Exception {
        // their own early answer (sent before the question opens) is untouched when nothing was aborted
        GameThread<Boolean> thread = new GameThread<>(n -> human.chooseUse(Outcome.Benefit, "Pay {1}?", null, game));
        thread.start();
        awaitAsked();
        Thread call = answer(() -> human.setResponseBoolean(true));
        Assert.assertTrue(thread.firstDone.await(5, TimeUnit.SECONDS));
        call.join(5_000);
        Assert.assertEquals(Boolean.TRUE, thread.first.get());

        thread.restore.countDown();
        Assert.assertTrue(thread.restored.await(5, TimeUnit.SECONDS));
        Thread early = answer(() -> human.setResponseBoolean(false)); // before the second question
        Thread.sleep(200);
        thread.goOn.countDown();
        thread.join(10_000);
        early.join(5_000);
        Assert.assertFalse(thread.isAlive());
        Assert.assertEquals(Boolean.FALSE, thread.second.get());
    }
}
