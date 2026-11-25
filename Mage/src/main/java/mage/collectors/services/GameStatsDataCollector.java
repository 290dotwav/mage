package mage.collectors.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mage.cards.Card;
import mage.cards.decks.Deck;
import mage.constants.CommanderCardType;
import mage.game.Game;
import mage.game.Table;
import mage.game.match.MatchPlayer;
import mage.players.Player;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The leaderboard's numbers: one JSON line per finished game, appended to a file
 * ({@code -Dxmage.stats.file}, default {@code stats/games.jsonl} under the server's folder,
 * which the ClaudeMTG recipe keeps on a volume). The web door serves them (GET /stats/games).
 * <p>
 * Enabled by {@code -Dxmage.dataCollectors.gameStats=true}; off by default. What is counted
 * during the game is {@link GameStatsWatcher}'s. A line:
 * <pre>
 * {"v":1,"gameId","tableId","tableName","scenario","startedAt","endedAt","turns","gameType",
 *  "winner","draw","unknownKills","players":[{"name","claude","ai","winner","quit",
 *  "eliminatedTurn","eliminatedBy","commanders","deck":[{"n","name"}],"side":[{"n","name"}],
 *  "deckHash","stats":{"damageToPlayers","damageToCreatures","creaturesKilled",
 *  "creaturesLost","spellsCast","lifeGained","playersEliminated"}}]}
 * </pre>
 * {@code claude}: the seat was taken by a socket the site did not send (the machine's own
 * Claude chairs, {@link #noteSeat}); {@code ai}: one of the server's own bots.
 */
public class GameStatsDataCollector extends EmptyDataCollector {

    private static final Logger logger = Logger.getLogger(GameStatsDataCollector.class);

    public static final String SERVICE_CODE = "gameStats";

    public static final int VERSION = 1;

    private static final String CONCEDED = " has conceded.";

    /** A table's name, scenario and game type, kept from its start for its games' lines. */
    private static final class TableInfo {
        final String name;
        final String scenario;
        final String gameType;

        TableInfo(String name, String scenario, String gameType) {
            this.name = name;
            this.scenario = scenario;
            this.gameType = gameType;
        }
    }

    private static final Map<UUID, TableInfo> tables = new ConcurrentHashMap<>();
    /** A table to the names seated at it by a socket the site did not send (true) or did (false). */
    private static final Map<UUID, Map<String, Boolean>> seatOrigins = new ConcurrentHashMap<>();
    /** A game to the players who conceded it (heard in its log). */
    private final Map<UUID, Set<UUID>> conceded = new ConcurrentHashMap<>();
    /** The games already written: a game ends once. */
    private final Set<UUID> written = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Path file;

    public GameStatsDataCollector() {
        this(Paths.get(System.getProperty("xmage.stats.file", "stats/games.jsonl")));
    }

    GameStatsDataCollector(Path file) {
        this.file = file;
    }

    /** The file the lines go to (the door reads it). */
    public static Path statsFile() {
        return Paths.get(System.getProperty("xmage.stats.file", "stats/games.jsonl"));
    }

    /**
     * The door says who took a seat: {@code claude} when the socket carried no account of the
     * site (the machine's own chairs). Kept until the table ends.
     */
    public static void noteSeat(UUID tableId, String name, boolean claude) {
        if (tableId == null || name == null) {
            return;
        }
        seatOrigins.computeIfAbsent(tableId, k -> new ConcurrentHashMap<>()).put(name, claude);
    }

    @Override
    public String getServiceCode() {
        return SERVICE_CODE;
    }

    @Override
    public String getInitInfo() {
        return "games to " + file.toAbsolutePath();
    }

    @Override
    public void onTableStart(Table table) {
        String scenario = table.getMatch() == null || table.getMatch().getOptions() == null ? null : table.getMatch().getOptions().getScenario();
        tables.put(table.getId(), new TableInfo(table.getName(), scenario, table.getGameType()));
    }

    @Override
    public void onTableEnd(Table table) {
        tables.remove(table.getId());
        seatOrigins.remove(table.getId());
    }

    @Override
    public void onGameStart(Game game) {
        if (game.getState().getWatcher(GameStatsWatcher.class) == null) {
            game.getState().addWatcher(new GameStatsWatcher());
        }
    }

    @Override
    public void onGameLog(Game game, String message) {
        if (message == null || !message.endsWith(CONCEDED)) {
            return;
        }
        for (Player player : game.getState().getPlayers().values()) {
            if (message.equals(player.getLogName() + CONCEDED)) {
                conceded.computeIfAbsent(game.getId(), k -> ConcurrentHashMap.newKeySet()).add(player.getId());
            }
        }
    }

    @Override
    public void onGameEndResult(Game game) {
        if (!written.add(game.getId())) {
            return;
        }
        try {
            JsonObject line = record(game, conceded.getOrDefault(game.getId(), Collections.emptySet()));
            append(line.toString());
        } catch (Throwable e) {
            // never break a game's end for a statistic
            logger.error("Game stats: could not write game " + game.getId() + ": " + e, e);
        } finally {
            conceded.remove(game.getId());
        }
    }

    private synchronized void append(String line) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** The game's line. {@code concededIds}: the players heard conceding it. */
    public static JsonObject record(Game game, Set<UUID> concededIds) {
        GameStatsWatcher stats = game.getState().getWatcher(GameStatsWatcher.class);
        TableInfo table = game.getTableId() == null ? null : tables.get(game.getTableId());
        Map<String, Boolean> origins = game.getTableId() == null ? null : seatOrigins.get(game.getTableId());

        Set<UUID> quit = new HashSet<>(concededIds);
        for (Player player : game.getState().getPlayers().values()) {
            if (player.hasQuit()) {
                quit.add(player.getId());
            }
        }

        JsonObject json = new JsonObject();
        json.addProperty("v", VERSION);
        json.addProperty("gameId", game.getId().toString());
        json.addProperty("tableId", game.getTableId() == null ? null : game.getTableId().toString());
        json.addProperty("tableName", table == null ? null : table.name);
        json.addProperty("scenario", table == null ? null : table.scenario);
        long endedAt = game.getEndTime() != null ? game.getEndTime().getTime() : System.currentTimeMillis();
        json.addProperty("startedAt", game.getStartTime() != null ? game.getStartTime().getTime() : endedAt);
        json.addProperty("endedAt", endedAt);
        json.addProperty("turns", game.getTurnNum());
        json.addProperty("gameType", table != null && table.gameType != null ? table.gameType
                : game.getGameType() == null ? null : game.getGameType().getName());
        String winner = null;
        boolean draw = game.isADraw();
        JsonArray players = new JsonArray();
        for (Player player : game.getState().getPlayers().values()) {
            boolean won = !draw && player.hasWon();
            if (won) {
                winner = player.getName();
            }
            JsonObject p = new JsonObject();
            p.addProperty("name", player.getName());
            Boolean claude = origins == null ? null : origins.get(player.getName());
            p.addProperty("claude", player.isHuman() && Boolean.TRUE.equals(claude));
            p.addProperty("ai", !player.isHuman());
            p.addProperty("winner", won);
            p.addProperty("quit", quit.contains(player.getId()));
            Integer eliminatedTurn = stats == null ? null : stats.getEliminatedTurn(player.getId());
            p.addProperty("eliminatedTurn", won ? null : eliminatedTurn);
            UUID by = stats == null || quit.contains(player.getId()) ? null : stats.getEliminatedBy(player.getId());
            Player byPlayer = by == null ? null : game.getPlayer(by);
            p.addProperty("eliminatedBy", byPlayer == null ? null : byPlayer.getName());

            JsonArray commanders = new JsonArray();
            List<String> commanderNames = new ArrayList<>();
            for (UUID id : game.getCommandersIds(player, CommanderCardType.COMMANDER_OR_OATHBREAKER, false)) {
                Card card = game.getCard(id);
                if (card != null) {
                    commanderNames.add(card.getName());
                }
            }
            Collections.sort(commanderNames);
            commanderNames.forEach(commanders::add);
            p.add("commanders", commanders);

            MatchPlayer matchPlayer = player.getMatchPlayer();
            Deck deck = matchPlayer == null ? null : matchPlayer.getDeck();
            SortedMap<String, Integer> main = count(deck == null ? Collections.emptySet() : deck.getCards());
            SortedMap<String, Integer> side = count(deck == null ? Collections.emptySet() : deck.getSideboard());
            p.add("deck", list(main));
            p.add("side", list(side));
            p.addProperty("deckHash", deckHash(main, side));

            JsonObject s = new JsonObject();
            UUID id = player.getId();
            s.addProperty("damageToPlayers", stats == null ? 0 : stats.getDamageToPlayers(id));
            s.addProperty("damageToCreatures", stats == null ? 0 : stats.getDamageToCreatures(id));
            s.addProperty("creaturesKilled", stats == null ? 0 : stats.getCreaturesKilled(id));
            s.addProperty("creaturesLost", stats == null ? 0 : stats.getCreaturesLost(id));
            s.addProperty("spellsCast", stats == null ? 0 : stats.getSpellsCast(id));
            s.addProperty("lifeGained", stats == null ? 0 : stats.getLifeGained(id));
            s.addProperty("playersEliminated", stats == null ? 0 : stats.getPlayersEliminated(id, quit));
            p.add("stats", s);
            players.add(p);
        }
        json.addProperty("winner", winner);
        json.addProperty("draw", draw);
        json.addProperty("unknownKills", stats == null ? 0 : stats.getUnknownKills());
        json.add("players", players);
        return json;
    }

    private static SortedMap<String, Integer> count(Collection<Card> cards) {
        SortedMap<String, Integer> counts = new TreeMap<>();
        for (Card card : cards) {
            counts.merge(card.getName(), 1, Integer::sum);
        }
        return counts;
    }

    private static JsonArray list(SortedMap<String, Integer> counts) {
        JsonArray array = new JsonArray();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            JsonObject card = new JsonObject();
            card.addProperty("n", entry.getValue());
            card.addProperty("name", entry.getKey());
            array.add(card);
        }
        return array;
    }

    /** The first 16 hex digits of SHA-256 over "n name" lines, main then "SB:" side, names sorted. */
    static String deckHash(SortedMap<String, Integer> main, SortedMap<String, Integer> side) {
        StringBuilder text = new StringBuilder();
        main.forEach((name, n) -> text.append(n).append(' ').append(name).append('\n'));
        side.forEach((name, n) -> text.append("SB: ").append(n).append(' ').append(name).append('\n'));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
