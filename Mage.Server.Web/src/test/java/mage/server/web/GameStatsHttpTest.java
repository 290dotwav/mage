package mage.server.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * GET /stats/games?since= (GameStatsHttp): after since, oldest first, at most the limit,
 * and a line cut by a crash left out.
 */
class GameStatsHttpTest {

    private static String game(String id, long endedAt) {
        return "{\"v\":1,\"gameId\":\"" + id + "\",\"endedAt\":" + endedAt + ",\"players\":[]}";
    }

    private static List<String> ids(JsonObject answer) {
        JsonArray games = answer.getAsJsonArray("games");
        String[] out = new String[games.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = games.get(i).getAsJsonObject().get("gameId").getAsString();
        }
        return Arrays.asList(out);
    }

    @Test
    void afterSinceOldestFirst() {
        List<String> lines = Arrays.asList(game("c", 300), game("a", 100), "", game("b", 200));
        assertEquals(Arrays.asList("a", "b", "c"), ids(GameStatsHttp.select(lines, 0, 500)));
        assertEquals(Arrays.asList("c"), ids(GameStatsHttp.select(lines, 200, 500)));
        assertEquals(Arrays.asList(), ids(GameStatsHttp.select(lines, 300, 500)));
    }

    @Test
    void atMostTheLimit() {
        List<String> lines = Arrays.asList(game("a", 1), game("b", 2), game("c", 3));
        assertEquals(Arrays.asList("a", "b"), ids(GameStatsHttp.select(lines, 0, 2)));
        assertEquals(500, GameStatsHttp.MAX_GAMES);
    }

    @Test
    void aBrokenLineIsLeftOut() {
        List<String> lines = Arrays.asList(game("a", 1), "{\"v\":1,\"gameId\":\"b\",\"endedAt\":2,\"pla", "[1]", "{\"gameId\":\"x\"}");
        assertEquals(Arrays.asList("a"), ids(GameStatsHttp.select(lines, 0, 500)));
    }

    @Test
    void sinceFromTheQuery() {
        assertEquals(0L, GameStatsHttp.since(null));
        assertEquals(0L, GameStatsHttp.since(""));
        assertEquals(1234L, GameStatsHttp.since("since=1234"));
        assertEquals(1234L, GameStatsHttp.since("x=1&since=1234"));
        assertEquals(0L, GameStatsHttp.since("since="));
        assertEquals(0L, GameStatsHttp.since("since=-5"));
        assertNull(GameStatsHttp.since("since=abc"));
    }
}
