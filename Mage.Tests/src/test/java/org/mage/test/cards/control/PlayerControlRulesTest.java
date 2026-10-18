package org.mage.test.cards.control;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.util.CardUtil;
import mage.view.GameView;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestMultiPlayerBase;

/**
 * web door fork: the rules of controlling another player (723) that every card doing it relies on
 * (Mindslaver, Worst Fears, Sorin Markov, Emrakul, the Promised End, Cruel Entertainment, The
 * Dominion Bracelet, Construct a Cosmic Cube, Word of Command, Opposition Agent), and what the
 * door's view says of them.
 * <p>
 * Four players, turn order A, D, C, B; each sees his two neighbours (range of influence one).
 * The test framework cannot play the controlled player's decisions, so these check who controls
 * whom and what the views carry.
 *
 * @author ClaudeMTG
 */
public class PlayerControlRulesTest extends CardTestMultiPlayerBase {

    private void worstFearsOnD() {
        // You control target player during that player's next turn. Exile Worst Fears.
        addCard(Zone.HAND, playerA, "Worst Fears"); // {7}{B}
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 8);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Worst Fears", playerD);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
    }

    @Test
    public void test_LastControlEffectWins() {
        // 723.1a: the last one created is the one that works (the first one used to win)
        worstFearsOnD();
        // {4}, {T}, Sacrifice Mindslaver: You control target player during that player's next turn.
        addCard(Zone.BATTLEFIELD, playerC, "Mindslaver");
        addCard(Zone.BATTLEFIELD, playerC, "Island", 4);
        activateAbility(1, PhaseStep.POSTCOMBAT_MAIN, playerC, "{4}, {T}, Sacrifice", playerD);
        waitStackResolved(1, PhaseStep.POSTCOMBAT_MAIN);

        runCode("D's turn is C's", 2, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertEquals(info, playerC.getId(), playerD.getTurnControlledBy());
            Assert.assertTrue(info, playerC.getPlayersUnderYourControl().contains(playerD.getId()));
            Assert.assertTrue(info, playerA.getPlayersUnderYourControl().isEmpty());
            Assert.assertEquals(info, playerC.getId(), getGameView(playerB).getTurnControllers().get(playerD.getId()));
        });

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_ControlOfHimselfOverwrites() {
        // 723.9 and 723.1a: a player given control of himself after it plays his own turn
        worstFearsOnD();
        addCard(Zone.BATTLEFIELD, playerD, "Mindslaver");
        addCard(Zone.BATTLEFIELD, playerD, "Island", 4);
        activateAbility(1, PhaseStep.POSTCOMBAT_MAIN, playerD, "{4}, {T}, Sacrifice", playerD);
        waitStackResolved(1, PhaseStep.POSTCOMBAT_MAIN);

        runCode("D plays his own turn", 2, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertTrue(info, playerD.isGameUnderControl());
            Assert.assertEquals(info, playerD.getId(), playerD.getTurnControlledBy());
            Assert.assertTrue(info, getGameView(playerA).getTurnControllers().isEmpty());
        });

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_EmrakulsExtraTurnStaysWhenAnotherControlWins() {
        // Emrakul's "after that turn, that player takes an extra turn" is its own: a later
        // control effect takes the turn (723.1a), not the extra one
        addCard(Zone.HAND, playerA, "Emrakul, the Promised End"); // {13}
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 13);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Emrakul, the Promised End");
        addTarget(playerA, playerD);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        addCard(Zone.BATTLEFIELD, playerC, "Mindslaver");
        addCard(Zone.BATTLEFIELD, playerC, "Island", 4);
        activateAbility(1, PhaseStep.POSTCOMBAT_MAIN, playerC, "{4}, {T}, Sacrifice", playerD);
        waitStackResolved(1, PhaseStep.POSTCOMBAT_MAIN);

        runCode("D's turn is C's", 2, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertEquals(info, playerC.getId(), playerD.getTurnControlledBy());
        });
        runCode("then D's extra turn, his own", 3, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertEquals(info, playerD.getId(), game.getActivePlayerId());
            Assert.assertTrue(info, playerD.isGameUnderControl());
        });

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_ControllerLeavesInTheMiddleOfTheTurn() {
        // 800.4a: when a player leaves the game, any effects which give that player control of
        // players end; D's questions went on to A's empty seat
        worstFearsOnD();
        concede(2, PhaseStep.PRECOMBAT_MAIN, playerA);

        runCode("D under A", 2, PhaseStep.UPKEEP, playerB, (info, player, game) -> {
            Assert.assertEquals(info, playerA.getId(), playerD.getTurnControlledBy());
        });
        runCode("A left: D plays his own turn again", 2, PhaseStep.POSTCOMBAT_MAIN, playerB, (info, player, game) -> {
            Assert.assertTrue(info, playerA.hasLeft());
            Assert.assertTrue(info, playerD.isGameUnderControl());
            Assert.assertEquals(info, playerD.getId(), playerD.getTurnControlledBy());
            Assert.assertTrue(info, getGameView(playerD).getTurnControllers().isEmpty());
        });

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_ControlledPlayerLeavesInTheMiddleOfHisTurn() {
        // 723.6: a controlled player may concede; he used to stay his controller's for the rest
        // of the game (the start of a turn lets go of players still in it only)
        worstFearsOnD();
        concede(2, PhaseStep.PRECOMBAT_MAIN, playerD);

        runCode("D left", 2, PhaseStep.POSTCOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertTrue(info, playerD.hasLeft());
            Assert.assertTrue(info, playerA.getPlayersUnderYourControl().isEmpty());
            GameView viewA = getGameView(playerA);
            Assert.assertTrue(info, viewA.getTurnControllers().isEmpty());
            Assert.assertTrue(info, viewA.getControlledViews().isEmpty());
        });
        runCode("and stays let go", 3, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertTrue(info, playerA.getPlayersUnderYourControl().isEmpty());
            Assert.assertTrue(info, getGameView(playerA).getTurnControllers().isEmpty());
        });

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_LimitedControlIsSaid() {
        // 723.2: Word of Command and Opposition Agent control a player for a limited duration,
        // the door's view says so (limitedControl); a whole turn (723.1) is not in it
        worstFearsOnD();

        runCode("a whole turn", 2, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            GameView viewA = getGameView(playerA);
            Assert.assertEquals(info, playerA.getId(), viewA.getTurnControllers().get(playerD.getId()));
            Assert.assertTrue(info, viewA.getLimitedControl().isEmpty());

            // A takes D again for a moment, the way Opposition Agent does while D searches
            CardUtil.takeControlUnderPlayerStart(game, null, playerA, playerD, false);
            Assert.assertTrue(info, getGameView(playerA).getLimitedControl().contains(playerD.getId()));
            CardUtil.takeControlUnderPlayerEnd(game, null, playerA, playerD);
            GameView after = getGameView(playerA);
            Assert.assertEquals(info, playerA.getId(), after.getTurnControllers().get(playerD.getId()));
            Assert.assertTrue(info, after.getLimitedControl().isEmpty());
        });
        runCode("a moment of another turn", 3, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertTrue(info, getGameView(playerA).getTurnControllers().isEmpty());
            // C's turn: A controls D for a moment only (Word of Command)
            CardUtil.takeControlUnderPlayerStart(game, null, playerA, playerD, false);
            GameView during = getGameView(playerA);
            Assert.assertEquals(info, playerA.getId(), during.getTurnControllers().get(playerD.getId()));
            Assert.assertTrue(info, during.getLimitedControl().contains(playerD.getId()));
            Assert.assertTrue(info, getGameView(playerB).getLimitedControl().contains(playerD.getId()));
            CardUtil.takeControlUnderPlayerEnd(game, null, playerA, playerD);
            Assert.assertTrue(info, getGameView(playerA).getTurnControllers().isEmpty());
            Assert.assertTrue(info, getGameView(playerA).getLimitedControl().isEmpty());
        });

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();
    }
}
