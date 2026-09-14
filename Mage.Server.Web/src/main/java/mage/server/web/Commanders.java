package mage.server.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.constants.CommanderCardType;
import mage.game.Game;
import mage.players.Player;
import mage.watchers.common.CommanderInfoWatcher;

import mage.server.managers.ManagerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Commander damage, per player, as a number the interface can read.
 * <p>
 * Nothing on their wire carries it: a PlayerView has a life and its counters, and the only
 * trace of the 21-damage clock is an English sentence the {@link CommanderInfoWatcher} pins on
 * the commander card itself ("<b>Commander</b> did 7 combat damage to player F-G."), which
 * reaches a client inside that CardView's info. Parsing it would mean matching players by
 * name - names repeat at a table, the text is localised, and what the interface needs is the
 * commander's id anyway. So the door reads the watchers instead and writes the numbers down,
 * the same way {@link StackControllers} writes a stack entry's controller: into the JSON tree
 * only, their view classes untouched.
 * <p>
 * Two fields are added to every entry of {@code players[]} of a GameView:
 * <pre>
 *   "commanderDamage": { "&lt;commander card id&gt;": 7, ... }   // combat damage THIS player has taken
 *   "commanderIds":    [ "&lt;commander card id&gt;", ... ]      // THIS player's own commanders, any zone
 * </pre>
 * Both keyed by the commander CARD id ({@code player.getCommandersIds()}, the id a
 * {@code CommanderView} in {@code commandList} carries), because rule 903.10a counts the 21 per
 * commander card: a pair of partners is two separate clocks, and a name would not tell them
 * apart. Zero is left out of {@code commanderDamage} - a player nobody has hit gets {@code {}}.
 * <p>
 * {@code commanderIds} comes with it because it is the only way the interface can name the
 * keys: their {@code commandList} holds a commander only while it sits in the command zone,
 * and a commander deals combat damage precisely when it does not - it is on the battlefield,
 * where it is an ordinary PermanentView with no mark on it.
 * <p>
 * Both are left out entirely for a game with no commanders (a duel), so an ordinary game's
 * frames are exactly what their view made. Read on the game thread like the stack above; a
 * concurrent change from another thread skips the enrichment for that frame.
 */
final class Commanders {

    private Commanders() {
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
            Map<String, List<String>> commanders = new HashMap<>();
            Map<String, Map<String, Integer>> damage = new HashMap<>();
            read(game, commanders, damage);
            if (commanders.isEmpty()) {
                return; // not a commander game: nothing to say, nothing added
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
                String playerId = idEl.getAsString();
                player.add("commanderIds", array(commanders.get(playerId)));
                player.add("commanderDamage", object(damage.get(playerId)));
            }
        } catch (RuntimeException ignore) {
            // the game changed under us (not the game thread): leave the frame as their view made it
        }
    }

    /**
     * Every seat's commanders, and the damage each of those commanders has dealt, turned round:
     * the watcher counts by damaged player, the interface wants it by damaged player's seat.
     */
    private static void read(Game game, Map<String, List<String>> commanders, Map<String, Map<String, Integer>> damage) {
        for (Player player : game.getPlayers().values()) {
            Set<UUID> ids = game.getCommandersIds(player, CommanderCardType.COMMANDER_OR_OATHBREAKER, false);
            if (ids.isEmpty()) {
                continue;
            }
            List<String> mine = new ArrayList<>();
            for (UUID commanderId : ids) {
                mine.add(commanderId.toString());
                // Only the commander watchers of a commander game exist; getWatcher logs a miss, so
                // this is reached only for ids their own state-based check reads the same way.
                CommanderInfoWatcher watcher = game.getState().getWatcher(CommanderInfoWatcher.class, commanderId);
                if (watcher == null) {
                    continue;
                }
                for (Map.Entry<UUID, Integer> hit : watcher.getDamageToPlayer().entrySet()) {
                    if (hit.getKey() == null || hit.getValue() == null || hit.getValue() <= 0) {
                        continue;
                    }
                    damage.computeIfAbsent(hit.getKey().toString(), k -> new LinkedHashMap<>())
                            .put(commanderId.toString(), hit.getValue());
                }
            }
            // Their own set is a HashSet: sorted here so a partner pair keeps the same order in
            // every frame, and the interface can tell the two clocks apart by position.
            mine.sort(String::compareTo);
            commanders.put(player.getId().toString(), mine);
        }
    }

    private static JsonArray array(List<String> values) {
        JsonArray out = new JsonArray();
        if (values != null) {
            for (String v : values) {
                out.add(v);
            }
        }
        return out;
    }

    private static JsonObject object(Map<String, Integer> values) {
        JsonObject out = new JsonObject();
        if (values != null) {
            for (Map.Entry<String, Integer> e : values.entrySet()) {
                out.addProperty(e.getKey(), e.getValue());
            }
        }
        return out;
    }
}
