package mage.server.web;

import com.google.gson.JsonObject;
import mage.game.Game;
import mage.game.command.Plane;
import mage.server.managers.ManagerFactory;
import mage.watchers.common.PlanarRollWatcher;

import java.util.UUID;

/**
 * The face-up plane of a Planechase game (901), and what the planar die costs right now, as
 * numbers the interface can read.
 * <p>
 * Their view carries the plane as a {@code PlaneView} in EVERY player's {@code commandList}
 * (planes are shared, {@code PlayerView}), with no field that says it is a plane rather than an
 * emblem, and nothing at all about the die's price: 901.9 makes the active player pay one more
 * mana for each roll he already made this turn, which their engine adds to the ability's cost
 * ({@code PlanarDieRollCostIncreasingEffect}) from a watcher, while the ability's text keeps
 * saying "{0}". So the door reads the live game, the same way {@link Commanders} does, and adds
 * one field to a GameView, into the JSON tree only:
 * <pre>
 *   "planechase": { "planeId": "&lt;the face-up plane's id&gt;", "dieCost": 1 }
 * </pre>
 * {@code dieCost} is what the active player would pay for his next roll this turn: their
 * watcher's count of the planar dice he rolled this turn, the number their cost effect adds.
 * Left out entirely when no plane is face up (any game that is not Planechase).
 */
final class Planechase {

    private Planechase() {
    }

    static void enrich(JsonObject frame, UUID gameId, ManagerFactory managerFactory) {
        JsonObject view = Frames.gameView(frame);
        if (view == null || !view.has("players")) {
            return;
        }
        try {
            Game game = LiveGame.of(gameId, managerFactory);
            if (game == null) {
                return;
            }
            Plane plane = game.getState().getCurrentPlane();
            if (plane == null) {
                return;
            }
            JsonObject out = new JsonObject();
            out.addProperty("planeId", plane.getId().toString());
            int cost = 0;
            UUID active = game.getActivePlayerId();
            PlanarRollWatcher watcher = game.getState().getWatcher(PlanarRollWatcher.class);
            if (watcher != null && active != null) {
                cost = watcher.getNumberTimesPlanarDieRolled(active);
            }
            out.addProperty("dieCost", cost);
            view.add("planechase", out);
        } catch (RuntimeException ignore) {
            // the game changed under us (not the game thread): leave the frame as their view made it
        }
    }
}
