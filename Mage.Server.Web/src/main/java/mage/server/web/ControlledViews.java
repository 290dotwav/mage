package mage.server.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.server.managers.ManagerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Turn control (723: Mindslaver, Emrakul, the Promised End, Sorin's emblem). The GameView of a
 * player who controls another's turn carries {@code controlledViews}: the game as each player
 * under his control sees it, built on the game thread ({@code GameSessionPlayer.controlledView}),
 * so the browser can sit him in that seat (its hand, its playables, what it may look at). Every
 * view carries {@code turnControllers} (controlled player id to controller id).
 * <p>
 * Those nested views are plain GameViews: the door's own enrichments
 * ({@link StackControllers}, {@link Commanders}) are given to the top-level view only, so this
 * gives them to each nested one as well, the same way, as if it were its own frame.
 */
final class ControlledViews {

    private ControlledViews() {
    }

    static void enrich(JsonObject frame, UUID gameId, ManagerFactory managerFactory) {
        JsonObject view = Frames.gameView(frame);
        JsonElement nested = view == null ? null : view.get("controlledViews");
        if (nested == null || !nested.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : nested.getAsJsonObject().entrySet()) {
            if (!e.getValue().isJsonObject()) {
                continue;
            }
            JsonObject own = new JsonObject();
            own.add("data", e.getValue());
            StackControllers.enrich(own, gameId, managerFactory);
            Commanders.enrich(own, gameId, managerFactory);
        }
    }
}
