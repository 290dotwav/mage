package mage.server.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mage.collectors.services.GameStatsDataCollector;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * The finished games' numbers for the site's leaderboard, over plain HTTP:
 * <pre>
 *     GET /stats/games?since=&lt;endedAt ms&gt;   -&gt;  {"games":[...]}
 * </pre>
 * the lines {@link GameStatsDataCollector} wrote, those that ended strictly after {@code since}
 * (0 or absent: all), sorted by {@code endedAt}, {@value #MAX_GAMES} at most: the caller asks
 * again from the last one it got.
 * <p>
 * Its own port ({@code -Dxmage.stats.port}, default {@value #DEFAULT_PORT}; 0 keeps it shut),
 * because the door's WebSocket listener answers no plain request. It is reached the way the door
 * is: Caddy's locked address, with the site's key, sends /stats/* here (ClaudeMTG
 * deploy/xmage/Caddyfile); the port itself is open on the containers' network only.
 */
final class GameStatsHttp {

    private static final Logger logger = Logger.getLogger(GameStatsHttp.class);

    static final int DEFAULT_PORT = 17174;
    static final int MAX_GAMES = 500;
    static final String PATH = "/stats/games";

    private GameStatsHttp() {
    }

    /** Opens the listener; null when it is off or the port cannot be taken (the door goes on without it). */
    static HttpServer start(String host, Path file) {
        int port = Integer.getInteger("xmage.stats.port", DEFAULT_PORT);
        if (port <= 0) {
            logger.info("Game stats: no HTTP listener (-Dxmage.stats.port=0)");
            return null;
        }
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext("/", exchange -> answer(exchange, file));
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "game-stats-http");
                thread.setDaemon(true);
                return thread;
            }));
            server.start();
            logger.info("Game stats: GET " + PATH + " on " + host + ":" + port + " from " + file.toAbsolutePath());
            return server;
        } catch (IOException | RuntimeException e) {
            logger.error("Game stats: could not listen on " + host + ":" + port + ": " + e, e);
            return null;
        }
    }

    private static void answer(HttpExchange exchange, Path file) throws IOException {
        try {
            URI uri = exchange.getRequestURI();
            if (!"GET".equals(exchange.getRequestMethod())) {
                send(exchange, 405, "{\"error\":\"GET only\"}");
                return;
            }
            if (!PATH.equals(uri.getPath())) {
                send(exchange, 404, "{\"error\":\"not here\"}");
                return;
            }
            Long since = since(uri.getRawQuery());
            if (since == null) {
                send(exchange, 400, "{\"error\":\"since must be a number of milliseconds\"}");
                return;
            }
            send(exchange, 200, select(readLines(file), since, MAX_GAMES).toString());
        } catch (Throwable e) {
            logger.error("Game stats: " + e, e);
            send(exchange, 500, "{\"error\":\"could not read the games\"}");
        }
    }

    private static List<String> readLines(Path file) throws IOException {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return Collections.emptyList();
        }
    }

    /** The query's {@code since}: 0 when absent, null when it is not a whole number. */
    static Long since(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return 0L;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            if (!"since".equals(key)) {
                continue;
            }
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            if (value.isEmpty()) {
                return 0L;
            }
            try {
                return Math.max(0L, Long.parseLong(value));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return 0L;
    }

    /**
     * {"games":[...]}: the lines that ended after {@code since}, oldest first, {@code max} at
     * most. A line that does not read (cut by a crash while written) is left out.
     */
    static JsonObject select(List<String> lines, long since, int max) {
        List<JsonObject> games = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            JsonObject game;
            try {
                JsonElement parsed = JsonParser.parseString(line);
                if (!parsed.isJsonObject()) {
                    continue;
                }
                game = parsed.getAsJsonObject();
            } catch (RuntimeException e) {
                continue;
            }
            JsonElement endedAt = game.get("endedAt");
            if (endedAt == null || !endedAt.isJsonPrimitive() || !endedAt.getAsJsonPrimitive().isNumber()) {
                continue;
            }
            if (endedAt.getAsLong() > since) {
                games.add(game);
            }
        }
        games.sort(Comparator.comparingLong(g -> g.get("endedAt").getAsLong()));
        JsonArray array = new JsonArray();
        for (int i = 0; i < games.size() && i < max; i++) {
            array.add(games.get(i));
        }
        JsonObject answer = new JsonObject();
        answer.add("games", array);
        return answer;
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
