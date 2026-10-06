package mage.server.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.game.Game;
import mage.game.stack.PaidCost;
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
 * <p>
 * Each entry also gets {@code manaPaid} (their own {@code paid} is a boolean, "its costs are
 * paid"), what the object was put on the stack for, read off the
 * same live object ({@link PaidCost}): {@code cost} the mana cost as paid ("{4}{G}{G}": commander
 * tax, reductions, X by its value; "" when cast without paying any mana), {@code spent} the mana
 * actually spent on it, one symbol per mana ("{R}{U}{G}{G}"), and {@code x} the value announced
 * for X when the cost had one. Left out for a triggered ability, a copy, and an activated ability
 * whose cost holds no mana: nothing was paid for those.
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
            Map<String, StackObject> objects = objects(gameId, managerFactory);
            if (objects == null) {
                return;
            }
            for (Map.Entry<String, JsonElement> e : stackEl.getAsJsonObject().entrySet()) {
                StackObject object = objects.get(e.getKey());
                if (object == null || !e.getValue().isJsonObject()) {
                    // unknown (object already resolved when read off the game thread): fields left out
                    continue;
                }
                JsonObject entry = e.getValue().getAsJsonObject();
                if (object.getControllerId() != null) {
                    entry.addProperty("controllerId", object.getControllerId().toString());
                }
                JsonObject paid = paid(object);
                if (paid != null) {
                    entry.add("manaPaid", paid);
                }
            }
        } catch (RuntimeException ignore) {
            // stack changed under us (not the game thread): leave the frame as their view made it
        }
    }

    private static Map<String, StackObject> objects(UUID gameId, ManagerFactory managerFactory) {
        Game game = LiveGame.of(gameId, managerFactory);
        if (game == null) {
            return null;
        }
        Map<String, StackObject> out = new HashMap<>();
        for (StackObject so : game.getStack()) {
            out.put(so.getId().toString(), so);
        }
        return out;
    }

    /** The {@code manaPaid} object of one stack entry, or null when nothing was paid for it. */
    static JsonObject paid(StackObject object) {
        PaidCost paid = PaidCost.of(object);
        if (paid == null) {
            return null;
        }
        JsonObject out = new JsonObject();
        out.addProperty("cost", paid.getCost());
        out.addProperty("spent", paid.getSpent());
        if (paid.getX() != null) {
            out.addProperty("x", paid.getX());
        }
        return out;
    }
}
