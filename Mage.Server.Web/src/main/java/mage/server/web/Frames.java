package mage.server.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.interfaces.callback.ClientCallback;

import java.util.Collection;
import java.util.UUID;

/**
 * The JSON frames of the wire (docs/XMAGE-WIRE.md, "The wire we add").
 * <p>
 * Server to browser: callback, error, joined, result, hello.
 * Gson is configured exactly like the GameView dumps the interface was written against
 * (serializeNulls), so a browser sees the same shape as /tmp/xmage-gameview-A.json.
 */
final class Frames {

    static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private Frames() {
    }

    /**
     * { kind: "callback", method, gameId (the callback's objectId: the game id for game
     * callbacks, the chat or table id for the others), messageId, data }
     */
    static JsonObject callback(ClientCallback call, Object data) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", "callback");
        o.addProperty("method", call.getMethod().name());
        o.addProperty("gameId", call.getObjectId() == null ? null : call.getObjectId().toString());
        o.addProperty("messageId", call.getMessageId());
        o.add("data", GSON.toJsonTree(data));
        return o;
    }

    /**
     * { kind: "error", message, id? } - id echoes the inbound frame's "id" when it had one.
     */
    static String error(String message, JsonElement id) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", "error");
        o.addProperty("message", message);
        withId(o, id);
        return GSON.toJson(o);
    }

    /**
     * { kind: "joined", tableId, playerId, name, reconnected, deckWarnings[], deckReplaced, id? }
     * reconnected: the name was already seated there, no new seat was taken (User.onReconnect
     * replays JOINED_TABLE, START_GAME, GAME_INIT and the open question). deckReplaced counts
     * the lines whose printing the importer chose by name ("[???:k]").
     */
    static String joined(UUID tableId, UUID playerId, String name, boolean reconnected, DeckText.Parsed deck, JsonElement id) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", "joined");
        o.addProperty("tableId", tableId.toString());
        o.addProperty("playerId", playerId == null ? null : playerId.toString());
        o.addProperty("name", name);
        o.addProperty("reconnected", reconnected);
        o.add("deckWarnings", GSON.toJsonTree(deck == null ? new java.util.ArrayList<String>() : deck.warnings));
        o.addProperty("deckReplaced", deck == null ? 0 : deck.replaced);
        withId(o, id);
        return GSON.toJson(o);
    }

    /**
     * { kind: "result", method, data, id? } - the return value of a "call" (null for void
     * methods) or of a "table" op (method is then "table.create" etc.).
     */
    static String result(String method, Object value, JsonElement id) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", "result");
        o.addProperty("method", method);
        o.add("data", GSON.toJsonTree(value));
        withId(o, id);
        return GSON.toJson(o);
    }

    /**
     * { kind: "hello", sessionId, version, testMode, methods[] } - first frame on a new socket.
     */
    static String hello(String sessionId, String version, boolean testMode, Collection<String> methods) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", "hello");
        o.addProperty("sessionId", sessionId);
        o.addProperty("version", version);
        o.addProperty("testMode", testMode);
        JsonArray arr = new JsonArray();
        for (String m : methods) {
            arr.add(m);
        }
        o.add("methods", arr);
        return GSON.toJson(o);
    }

    private static void withId(JsonObject o, JsonElement id) {
        if (id != null && !id.isJsonNull()) {
            o.add("id", id);
        }
    }

    // --- reading inbound frames ---

    static String optString(JsonObject frame, String key, String def) {
        JsonElement e = frame.get(key);
        if (e == null || e.isJsonNull()) {
            return def;
        }
        return e.isJsonPrimitive() ? e.getAsString() : e.toString();
    }

    static String requireString(JsonObject frame, String key) {
        String s = optString(frame, key, null);
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException("frame needs a non-empty '" + key + "'");
        }
        return s;
    }

    static int optInt(JsonObject frame, String key, int def) {
        JsonElement e = frame.get(key);
        if (e == null || e.isJsonNull()) {
            return def;
        }
        return e.getAsInt();
    }
}
