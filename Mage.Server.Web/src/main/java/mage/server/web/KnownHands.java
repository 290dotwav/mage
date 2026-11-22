package mage.server.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.cards.Card;
import mage.game.Game;
import mage.players.Player;
import mage.server.managers.ManagerFactory;
import mage.view.CardView;

import java.util.UUID;

/**
 * The cards of a hand the whole table knows: revealed, then put into that hand (a tutor's
 * "reveal it and put it into your hand", a card revealed off the top then drawn). The owner:
 * « une carte qui a été révélée puis mise dans une main ensuite doit rester révélée. si je clique
 * sur la main qui l'a je peux la lire ». Their PlayerView carries only a hand's count, and their
 * revealed piles are emptied after the next priority, so the engine keeps what was revealed
 * ({@code GameState.noteRevealed}, {@code isKnownInHand}): a card stays known while it stays in
 * that hand, and is unknown again once it leaves it (cast, discarded, shuffled into a library).
 * <p>
 * Each entry of {@code players[]} gets {@code knownHand}: the CardViews of those cards, written
 * only when there is one, read off the live game like {@link StackControllers}.
 */
final class KnownHands {

    private KnownHands() {
    }

    static void enrich(JsonObject frame, UUID gameId, ManagerFactory managerFactory) {
        JsonObject view = Frames.gameView(frame);
        if (view == null) {
            return;
        }
        JsonElement playersEl = view.get("players");
        if (playersEl == null || !playersEl.isJsonArray() || playersEl.getAsJsonArray().size() == 0) {
            return;
        }
        try {
            Game game = LiveGame.of(gameId, managerFactory);
            if (game == null) {
                return;
            }
            for (JsonElement el : playersEl.getAsJsonArray()) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject entry = el.getAsJsonObject();
                JsonElement idEl = entry.get("playerId");
                if (idEl == null || !idEl.isJsonPrimitive()) {
                    continue;
                }
                Player player = game.getPlayer(UUID.fromString(idEl.getAsString()));
                if (player == null) {
                    continue;
                }
                JsonArray known = new JsonArray();
                for (Card card : player.getHand().getCards(game)) {
                    if (game.getState().isKnownInHand(card)) {
                        known.add(Frames.GSON.toJsonTree(new CardView(card, game, false)));
                    }
                }
                if (known.size() > 0) {
                    entry.add("knownHand", known);
                }
            }
        } catch (RuntimeException ignore) {
            // the game changed under us (not the game thread): leave the frame as their view made it
        }
    }
}
