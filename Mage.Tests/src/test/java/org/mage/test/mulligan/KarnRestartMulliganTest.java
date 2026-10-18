package org.mage.test.mulligan;

import mage.constants.CommanderCardType;
import mage.constants.MultiplayerAttackOption;
import mage.constants.PhaseStep;
import mage.constants.RangeOfInfluence;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.CommanderFreeForAll;
import mage.game.Game;
import mage.game.GameException;
import mage.game.RollbackPoints;
import mage.game.mulligan.LondonMulligan;
import mage.watchers.common.CommanderPlaysCountWatcher;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommander4Players;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Karn Liberated's -14 restarts the game (727): the restarted game's hands
 * are decided by everyone at once, and the server takes an answer only from
 * {@code game.getPriorityPlayerId()}, or from anybody while that is null
 * ({@code GameController.sendMessage}). That getter falls back on the active
 * player, and the restart kept the old game's: on a web door every seat but
 * Karn's controller had its keep dropped on the way in, and the restarted
 * game waited on its mulligans for ever.
 * <p>
 * Four seats of Commander, as the table plays them: the restart leaves no
 * player whose answer alone is taken, the commander with no tax, the old
 * game's monarch gone, the permanent Karn exiled onto the battlefield under
 * its controller, and no point of the old game left to go back to (their
 * turn numbers start over with the new game). A commander put in by the
 * test's cheat is a plain card of the new game ({@code KarnLiberatedEffect}),
 * so the command zone is checked on a real door with real decks (the site's
 * {@code packages/agent/harness/xmage-karn-restart.ts}).
 */
public class KarnRestartMulliganTest extends CardTestCommander4Players {

    /** What the server would read as "the priority player" each time the hands are asked about. */
    private final List<String> askedWith = Collections.synchronizedList(new ArrayList<>());
    /** The newest point to go back to taken in the old game. */
    private int lastOldPoint = -1;

    @Override
    protected Game createNewGameAndPlayers() throws GameException, FileNotFoundException {
        LondonMulligan recording = new LondonMulligan(0) {
            @Override
            public void executeMulliganPhase(Game game, int startingHandSize) {
                UUID priority = game.getPriorityPlayerId();
                askedWith.add(priority == null ? "none" : game.getPlayer(priority).getName());
                super.executeMulliganPhase(game, startingHandSize);
            }
        };
        Game game = new CommanderFreeForAll(MultiplayerAttackOption.MULTIPLE, RangeOfInfluence.ALL, recording, 20, 7);
        // Player order: A -> D -> C -> B
        playerA = createPlayer(game, "PlayerA");
        playerB = createPlayer(game, "PlayerB");
        playerC = createPlayer(game, "PlayerC");
        playerD = createPlayer(game, "PlayerD");
        return game;
    }

    @Test
    public void test_RestartedGame_NobodysAnswerAloneIsTaken() {
        addCard(Zone.COMMAND, playerA, "Balduvian Bears", 1); // {1}{G}, commander
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        // +4: Target player exiles a card from their hand.
        // -3: Exile target permanent.
        // -14: Restart the game, leaving in exile all non-Aura permanent cards exiled with Karn Liberated.
        // Then put those cards onto the battlefield under your control.
        addCard(Zone.BATTLEFIELD, playerA, "Karn Liberated", 1);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears", 1);

        // the commander cast once: tax {2} from now on
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Balduvian Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA);
        addCounters(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Karn Liberated", CounterType.LOYALTY, 30);
        // Karn exiles the other player's bears
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "-3: ", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA);
        runCode("the old game's monarch", 1, PhaseStep.PRECOMBAT_MAIN, playerA,
                (info, player, game) -> game.getState().setMonarchId(playerB.getId()));

        // A's next turn: the restart, with A the active player
        runCode("the old game's points to go back to", 5, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            for (RollbackPoints.Point point : game.getRollbackPoints().list()) {
                lastOldPoint = Math.max(lastOldPoint, point.getId());
            }
        });
        activateAbility(5, PhaseStep.PRECOMBAT_MAIN, playerA, "-14: ");

        setStopAt(5, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        Assert.assertEquals("the first game and the restarted one both asked about the hands", 2, askedWith.size());
        Assert.assertEquals("nobody's answer alone is taken while the hands are decided, restarted or not",
                Collections.nCopies(2, "none"), askedWith);

        UUID commanderId = currentGame.getCommandersIds(playerA, CommanderCardType.ANY, false).stream().findFirst().orElse(null);
        CommanderPlaysCountWatcher watcher = currentGame.getState().getWatcher(CommanderPlaysCountWatcher.class);
        Assert.assertEquals("commander tax reset", 0, watcher.getPlaysCount(commanderId));
        Assert.assertNull("the old game's monarch is gone", currentGame.getMonarchId());
        // the bears Karn exiled, onto the battlefield under Karn's controller
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerB, "Grizzly Bears", 0);
        assertPermanentCount(playerA, "Karn Liberated", 0);

        Assert.assertTrue("the old game had points to go back to", lastOldPoint >= 0);
        for (RollbackPoints.Point point : currentGame.getRollbackPoints().list()) {
            Assert.assertTrue("no point of the old game is offered in the new one: " + point.getLabel() + " (turn " + point.getTurn() + ")",
                    point.getId() > lastOldPoint);
        }
        Assert.assertFalse("the new game has its own points", currentGame.getRollbackPoints().list().isEmpty());
    }
}
