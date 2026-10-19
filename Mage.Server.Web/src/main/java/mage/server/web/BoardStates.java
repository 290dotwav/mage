package mage.server.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.abilities.Abilities;
import mage.abilities.Ability;
import mage.abilities.effects.common.ChooseSecretOpponentEffect;
import mage.abilities.keyword.HexproofAbility;
import mage.abilities.keyword.ProtectionFromEverythingAbility;
import mage.abilities.keyword.ShroudAbility;
import mage.constants.SubType;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.server.managers.ManagerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * States of the board that their GameView does not carry, as the interface reads them.
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
 * A player's protection from everything (702.16j: Teferi's Protection, The One Ring): nobody
 * can target him, damage to him is prevented. Their PlayerView says nothing of a player's own
 * abilities, so the screens of the players attacking him showed nothing.
 * <pre>
 *   "protectionFromEverything": true // on that player's entry of players[]; left out otherwise
 * </pre>
 * A player's hexproof and shroud (702.11c, 702.18a: Leyline of Sanctity, Shalai, True Believer):
 * his opponents (hexproof), or nobody (shroud), can target him. The same silence in their view.
 * <pre>
 *   "hexproof": true                 // on that player's entry of players[]; left out otherwise
 *   "shroud": true                   // the same
 * </pre>
 * A player's speed (702.179, Aetherdrift's "Start your engines!"): 1 to 4, what his max speed
 * abilities wait for. Their PlayerView names the designation ("Speed") and not its number.
 * <pre>
 *   "speed": 2                       // on that player's entry of players[]; left out until he has speed
 * </pre>
 * A secret choice of an opponent (Stalking Leonin, Guardian Archon, Emissary of Grudges): the
 * player who chose knows whom he chose, nobody else does until he reveals it. Their view says
 * only "X has secretly chosen an opponent." to everyone, the chooser too, so in a long game he
 * had nothing to read it back from. Only on the frame of the player who chose:
 * <pre>
 *   "secretOpponent": "&lt;playerId&gt;"  // on that permanent's entry; left out on every other frame
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
            JsonElement me = view.get("myPlayerId");
            UUID viewer = me != null && me.isJsonPrimitive() ? UUID.fromString(me.getAsString()) : null;
            JsonElement playersEl = view.get("players");
            if (playersEl == null || !playersEl.isJsonArray()) {
                return;
            }
            for (JsonElement el : playersEl.getAsJsonArray()) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonElement idEl = el.getAsJsonObject().get("playerId");
                if (idEl != null && idEl.isJsonPrimitive()) {
                    Player player = game.getPlayer(UUID.fromString(idEl.getAsString()));
                    if (player != null) {
                        markProtection(el.getAsJsonObject(), player.getAbilities());
                        markSpeed(el.getAsJsonObject(), player.getSpeed());
                    }
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
                    if (permanent == null) {
                        continue;
                    }
                    if (permanent.hasSubtype(SubType.CLASS, game)) {
                        e.getValue().getAsJsonObject().addProperty("classLevel", permanent.getClassLevel());
                    }
                    // the keys ChooseSecretOpponentEffect keeps them under: the permanent's id and zone change counter
                    String key = permanent.getId() + "_" + permanent.getZoneChangeCounter(game);
                    markSecret(e.getValue().getAsJsonObject(), viewer,
                            game.getState().getValue(key + ChooseSecretOpponentEffect.SECRET_OWNER),
                            game.getState().getValue(key + ChooseSecretOpponentEffect.SECRET_OPPONENT));
                }
            }
        } catch (RuntimeException ignore) {
            // the game changed under us (not the game thread): leave the frame as their view made it
        }
    }

    /** `speed` on a player's entry once he has any (702.179a: it starts at 1, never above 4). */
    static void markSpeed(JsonObject entry, int speed) {
        if (speed > 0) {
            entry.addProperty("speed", speed);
        }
    }

    /** `secretOpponent` on a permanent's entry, for the player who chose it only. */
    static void markSecret(JsonObject entry, UUID viewer, Object owner, Object chosen) {
        if (viewer != null && viewer.equals(owner) && chosen instanceof UUID) {
            entry.addProperty("secretOpponent", chosen.toString());
        }
    }

    /** `protectionFromEverything`, `hexproof`, `shroud` on a player's entry when he has that ability now (a layer gives it him, 613). */
    static void markProtection(JsonObject entry, Abilities<Ability> abilities) {
        if (abilities != null && abilities.containsClass(ProtectionFromEverythingAbility.class)) {
            entry.addProperty("protectionFromEverything", true);
        }
        // a player's hexproof or shroud (Leyline of Sanctity, True Believer), given him through a layer like the above
        if (abilities != null && abilities.containsClass(HexproofAbility.class)) {
            entry.addProperty("hexproof", true);
        }
        if (abilities != null && abilities.containsClass(ShroudAbility.class)) {
            entry.addProperty("shroud", true);
        }
    }
}
