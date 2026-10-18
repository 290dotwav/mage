package mage.server.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.game.Game;
import mage.server.managers.ManagerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * The cards a question hands over ({@code cardsView1}, {@code cardsView2} of a GAME_TARGET,
 * GAME_CHOOSE_PILE...) say nothing of whose they are: their CardView has no owner. A hand
 * revealed by Duress or Thoughtseize, the piles of an opponent's Fact or Fiction, the top of
 * an opponent's library under Gonti came to the asking player as if they were his own, so a
 * chair read "Lightning Bolt (hand)" while choosing from his opponent's hand. Each card entry
 * of those lists gets the owner's UUID, read on the server from the live game
 * ({@code game.getOwnerId}), and added to the JSON tree only:
 * <pre>
 *   "ownerId": "uuid"   // on each card of cardsView1 / cardsView2; left out when unknown
 * </pre>
 * Read on the game thread; a concurrent change skips the enrichment for that frame.
 */
final class DialogOwners {

    private DialogOwners() {
    }

    static void enrich(JsonObject frame, UUID gameId, ManagerFactory managerFactory) {
        JsonElement data = frame == null ? null : frame.get("data");
        if (data == null || !data.isJsonObject()) {
            return;
        }
        JsonObject message = data.getAsJsonObject();
        if (!message.has("cardsView1") && !message.has("cardsView2")) {
            return;
        }
        try {
            Game game = LiveGame.of(gameId, managerFactory);
            if (game == null) {
                return;
            }
            Function<UUID, UUID> ownerOf = game::getOwnerId;
            mark(message.get("cardsView1"), ownerOf);
            mark(message.get("cardsView2"), ownerOf);
        } catch (RuntimeException ignore) {
            // the game changed under us (not the game thread): leave the frame as their view made it
        }
    }

    /** Each card of one {@code cardsView} object, by its id: its owner, when the game knows it. */
    static void mark(JsonElement cards, Function<UUID, UUID> ownerOf) {
        if (cards == null || !cards.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : cards.getAsJsonObject().entrySet()) {
            if (!e.getValue().isJsonObject()) {
                continue;
            }
            UUID id;
            try {
                id = UUID.fromString(e.getKey());
            } catch (IllegalArgumentException notAnId) {
                continue;
            }
            UUID owner = ownerOf.apply(id);
            if (owner != null) {
                e.getValue().getAsJsonObject().addProperty("ownerId", owner.toString());
            }
        }
    }
}
