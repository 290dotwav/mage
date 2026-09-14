package mage.server.web;

import mage.game.Game;
import mage.game.Table;
import mage.server.game.GameController;
import mage.server.managers.ManagerFactory;

import java.util.UUID;

/**
 * The running {@link Game} behind a game id, for the enrichments the door adds to a frame
 * ({@link StackControllers}, {@link Commanders}) - facts their view classes do not carry and
 * that can only be read off the live game.
 * <p>
 * Game callbacks are fired on the game thread, which also owns this state, so a read from
 * there is consistent; from any other thread (a reconnect replay) a concurrent change is
 * possible and each caller simply skips its enrichment for that frame.
 */
final class LiveGame {

    private LiveGame() {
    }

    /** The game with that id, or null when the door cannot reach it (no controller, no table, already over). */
    static Game of(UUID gameId, ManagerFactory managerFactory) {
        if (gameId == null) {
            return null;
        }
        GameController controller = managerFactory.gameManager().getGameController().get(gameId);
        if (controller == null) {
            return null;
        }
        Table table = managerFactory.tableManager().getTable(controller.getTableId());
        if (table == null || table.getMatch() == null) {
            return null;
        }
        Game game = table.getMatch().getGame();
        if (game == null || !gameId.equals(game.getId())) {
            return null;
        }
        return game;
    }
}
