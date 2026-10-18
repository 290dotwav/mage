package mage.server.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.cards.Card;
import mage.cards.Cards;
import mage.game.Game;

import mage.server.managers.ManagerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Each player's companion (702.139a), for the whole game.
 * <p>
 * Their GameView lists a companion only while it is still outside the game ("Only show the
 * companion window when the companion is still outside the game"), so a screen that loads after
 * its owner paid {3} and put it into his hand cannot know there was one. The web table draws a
 * companion pile on every mat as soon as one seat has one, and keeps it for the game; a reload
 * must draw the same column. So the door names it, like {@link Commanders} names commanders:
 * <pre>
 *   "companionId": "&lt;card id&gt;"   // on THIS player's entry of players[], whatever zone it is in now
 * </pre>
 * Left out for a player without one, so a game with no companion is exactly what their view
 * made. Read on the game thread; a concurrent change skips the enrichment for that frame.
 */
final class Companions {

    private Companions() {
    }

    static void enrich(JsonObject frame, UUID gameId, ManagerFactory managerFactory) {
        JsonObject view = Frames.gameView(frame);
        if (view == null) {
            return;
        }
        JsonElement playersEl = view.get("players");
        if (playersEl == null || !playersEl.isJsonArray()) {
            return;
        }
        try {
            Game game = LiveGame.of(gameId, managerFactory);
            if (game == null || game.getState().getCompanion().isEmpty()) {
                return;
            }
            Map<String, String> byOwner = new HashMap<>();
            for (Cards cards : game.getState().getCompanion().values()) {
                for (Card card : cards.getCards(game)) {
                    if (card != null && card.getOwnerId() != null) {
                        byOwner.put(card.getOwnerId().toString(), card.getId().toString());
                    }
                }
            }
            for (JsonElement el : playersEl.getAsJsonArray()) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject player = el.getAsJsonObject();
                JsonElement idEl = player.get("playerId");
                if (idEl == null || !idEl.isJsonPrimitive()) {
                    continue;
                }
                String companion = byOwner.get(idEl.getAsString());
                if (companion != null) {
                    player.addProperty("companionId", companion);
                }
            }
        } catch (RuntimeException ignore) {
            // the game changed under us (not the game thread): leave the frame as their view made it
        }
    }
}
