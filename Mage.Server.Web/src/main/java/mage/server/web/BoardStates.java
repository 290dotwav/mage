package mage.server.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.constants.SubType;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.server.managers.ManagerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Two states of the board that their GameView does not carry, as the interface reads them.
 * <p>
 * Day and night (731): once a game has it, every player must know which it is, since it decides
 * how the next turn changes it and how a daybound or nightbound permanent enters. Their GameView
 * has no field for it, so a screen saw only a Brutal Cathar that had become Moonrage Brute.
 * <pre>
 *   "dayNight": "day" | "night"     // on the view; left out until the game has day and night
 * </pre>
 * A Class's level (716): what its level abilities give depends on it, and their PermanentView
 * has no field for it either (the level lives on the permanent, {@code getClassLevel}).
 * <pre>
 *   "classLevel": 2                  // on that permanent's entry of a player's battlefield
 * </pre>
 * Read on the game thread; a concurrent change skips the enrichment for that frame.
 */
final class BoardStates {

    private BoardStates() {
    }

    static void enrich(JsonObject frame, UUID gameId, ManagerFactory managerFactory) {
        JsonObject view = Frames.gameView(frame);
        if (view == null) {
            return;
        }
        try {
            Game game = LiveGame.of(gameId, managerFactory);
            if (game == null) {
                return;
            }
            if (game.hasDayNight()) {
                view.addProperty("dayNight", game.checkDayNight(true) ? "day" : "night");
            }
            JsonElement playersEl = view.get("players");
            if (playersEl == null || !playersEl.isJsonArray()) {
                return;
            }
            for (JsonElement el : playersEl.getAsJsonArray()) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonElement bf = el.getAsJsonObject().get("battlefield");
                if (bf == null || !bf.isJsonObject()) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> e : bf.getAsJsonObject().entrySet()) {
                    if (!e.getValue().isJsonObject()) {
                        continue;
                    }
                    Permanent permanent = game.getPermanent(UUID.fromString(e.getKey()));
                    if (permanent != null && permanent.hasSubtype(SubType.CLASS, game)) {
                        e.getValue().getAsJsonObject().addProperty("classLevel", permanent.getClassLevel());
                    }
                }
            }
        } catch (RuntimeException ignore) {
            // the game changed under us (not the game thread): leave the frame as their view made it
        }
    }
}
