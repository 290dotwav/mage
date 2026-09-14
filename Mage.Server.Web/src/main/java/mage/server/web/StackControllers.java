package mage.server.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.game.Game;
import mage.game.stack.StackObject;
import mage.server.managers.ManagerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A GameView does not say who controls an object on the stack (a spell's CardView has no
 * controller). Asked by the interface (docs/XMAGE-WIRE.md, piece B): each entry of
 * {@code stack{}} gets a {@code controllerId} (the controlling player's UUID), read on the
 * server from the live game ({@code game.getStack()} -> {@code StackObject.getControllerId()})
 * and added to the JSON tree only - their view classes are untouched.
 * <p>
 * Game callbacks are fired on the game thread, which also owns the stack, so the read is
 * consistent; from any other thread (a reconnect replay) a concurrent change is possible
 * and the enrichment is simply skipped for that frame.
 */
final class StackControllers {

    private StackControllers() {
    }

    static void enrich(JsonObject frame, UUID gameId, ManagerFactory managerFactory) {
        JsonObject view = Frames.gameView(frame);
        if (view == null) {
            return;
        }
        JsonElement stackEl = view.get("stack");
        if (stackEl == null || !stackEl.isJsonObject() || stackEl.getAsJsonObject().size() == 0) {
            return;
        }
        try {
            Map<String, String> controllers = controllers(gameId, managerFactory);
            if (controllers == null) {
                return;
            }
            for (Map.Entry<String, JsonElement> e : stackEl.getAsJsonObject().entrySet()) {
                String controllerId = controllers.get(e.getKey());
                if (controllerId != null && e.getValue().isJsonObject()) {
                    // unknown (object already resolved when read off the game thread): field left out
                    e.getValue().getAsJsonObject().addProperty("controllerId", controllerId);
                }
            }
        } catch (RuntimeException ignore) {
            // stack changed under us (not the game thread): leave the frame as their view made it
        }
    }

    private static Map<String, String> controllers(UUID gameId, ManagerFactory managerFactory) {
        Game game = LiveGame.of(gameId, managerFactory);
        if (game == null) {
            return null;
        }
        Map<String, String> out = new HashMap<>();
        for (StackObject so : game.getStack()) {
            out.put(so.getId().toString(), so.getControllerId() == null ? null : so.getControllerId().toString());
        }
        return out;
    }
}
