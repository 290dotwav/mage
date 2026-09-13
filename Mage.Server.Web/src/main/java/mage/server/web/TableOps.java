package mage.server.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.MageException;
import mage.constants.MatchBufferTime;
import mage.constants.MatchTimeLimit;
import mage.constants.MultiplayerAttackOption;
import mage.constants.RangeOfInfluence;
import mage.constants.SkillLevel;
import mage.game.Seat;
import mage.game.Table;
import mage.game.match.MatchOptions;
import mage.game.mulligan.MulliganType;
import mage.players.PlayerType;
import mage.players.net.UserData;
import mage.server.MageServerImpl;
import mage.server.Main;
import mage.server.Session;
import mage.server.managers.ManagerFactory;
import mage.view.TableView;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "table" frames, the three operations a browser needs before a game exists:
 * <pre>
 * { kind: "table", op: "create", gameType: "Commander Free For All", seats: 4, deck: "&lt;.dck text&gt;", name: "Alice",
 *   seatTypes?: ["Human","Computer - mad","Computer - mad","Computer - mad"], tableName?: "FG", deckType?: "Variant Magic - Commander" }
 * { kind: "table", op: "join",   tableId, deck, name, playerType?: "Human" | "Computer - mad", skill?: 2, password?: "" }
 * { kind: "table", op: "start",  tableId }
 * </pre>
 * "name" is the seat's player name. On a socket that has not connected a user yet, create
 * and a human join connect one under that name first (anonymous mode, as their own client
 * does with connectUser + connectSetUserData). Seats are typed when the table is created
 * (Table.getNextAvailableSeat matches the PlayerType), hence seatTypes; default all Human.
 * create also seats its creator with the given deck, like their NewTableDialog does.
 */
final class TableOps {

    private static final Logger logger = Logger.getLogger(TableOps.class);

    private final ManagerFactory managerFactory;
    private final MageServerImpl server;

    TableOps(ManagerFactory managerFactory, MageServerImpl server) {
        this.managerFactory = managerFactory;
        this.server = server;
    }

    void handle(WebSession session, JsonObject frame, JsonElement id) throws Exception {
        String op = Frames.requireString(frame, "op");
        switch (op) {
            case "create":
                create(session, frame, id);
                break;
            case "join":
                join(session, frame, id);
                break;
            case "start":
                start(session, frame, id);
                break;
            default:
                throw new IllegalArgumentException("unknown table op '" + op + "' (create, join, start)");
        }
    }

    private UUID roomId() throws MageException {
        return server.serverGetMainRoomId();
    }

    private Session session(WebSession web) {
        Session s = managerFactory.sessionManager().getSession(web.sessionId).orElse(null);
        if (s == null) {
            throw new IllegalStateException("server session " + web.sessionId + " is gone, reconnect");
        }
        return s;
    }

    private void ensureConnected(WebSession web, String name) throws MageException {
        if (session(web).getUserId() != null) {
            return;
        }
        if (!server.connectUser(name, "", web.sessionId, "", Main.getVersion(), "web")) {
            throw new MageException("connect refused for user '" + name + "' (the reason came as a SHOW_USERMESSAGE callback)");
        }
        server.connectSetUserData(web.sessionId, UserData.getDefaultUserDataView(), Main.getVersion().toString(), "web");
        logger.info("Web door: " + web.sessionId + " connected as " + name);
    }

    static String defaultDeckType(String gameType) {
        if (gameType.startsWith("Freeform Unlimited Commander")) {
            return "Variant Magic - Freeform Unlimited Commander";
        }
        if (gameType.startsWith("Freeform Commander")) {
            return "Variant Magic - Freeform Commander";
        }
        if (gameType.startsWith("Penny Dreadful Commander")) {
            return "Variant Magic - Penny Dreadful Commander";
        }
        if (gameType.startsWith("Commander")) {
            return "Variant Magic - Commander";
        }
        if (gameType.startsWith("Brawl")) {
            return "Variant Magic - Brawl";
        }
        if (gameType.startsWith("Oathbreaker")) {
            return "Variant Magic - Oathbreaker";
        }
        if (gameType.startsWith("Tiny Leaders")) {
            return "Variant Magic - Tiny Leaders";
        }
        return "Constructed - Freeform";
    }

    private void create(WebSession web, JsonObject frame, JsonElement id) throws Exception {
        String gameType = Frames.optString(frame, "gameType", "Commander Free For All");
        int seats = Frames.optInt(frame, "seats", 4);
        String name = Frames.requireString(frame, "name");
        String tableName = Frames.optString(frame, "tableName", name);
        String deckType = Frames.optString(frame, "deckType", defaultDeckType(gameType));
        List<PlayerType> seatTypes = new ArrayList<>();
        JsonElement st = frame.get("seatTypes");
        if (st != null && st.isJsonArray()) {
            for (JsonElement e : (JsonArray) st) {
                seatTypes.add((PlayerType) Wire.enumValue(PlayerType.class, e.getAsString()));
            }
            if (seatTypes.size() != seats) {
                throw new IllegalArgumentException("seatTypes has " + seatTypes.size() + " entries for " + seats + " seats");
            }
        } else {
            for (int i = 0; i < seats; i++) {
                seatTypes.add(PlayerType.HUMAN);
            }
        }
        if (!seatTypes.contains(PlayerType.HUMAN)) {
            throw new IllegalArgumentException("the creator needs a Human seat");
        }
        DeckText.Parsed deck = DeckText.parse(Frames.requireString(frame, "deck"));

        ensureConnected(web, name);

        MatchOptions options = new MatchOptions(tableName, gameType, false);
        options.getPlayerTypes().addAll(seatTypes);
        options.setDeckType(deckType);
        options.setLimited(false);
        options.setAttackOption(MultiplayerAttackOption.MULTIPLE);
        options.setRange(RangeOfInfluence.ALL);
        options.setWinsNeeded(1);
        options.setFreeMulligans(0);
        options.setMullgianType(MulliganType.GAME_DEFAULT);
        options.setMatchTimeLimit(MatchTimeLimit.NONE);
        options.setMatchBufferTime(MatchBufferTime.NONE);
        options.setSkillLevel(SkillLevel.CASUAL);
        options.setRollbackTurnsAllowed(false);
        options.setSpectatorsAllowed(true);
        options.setRated(false);
        options.setPassword("");
        options.setQuitRatio(100);
        options.setMinimumRating(0);
        options.setEdhPowerLevel(0);

        UUID roomId = roomId();
        TableView table = server.roomCreateTable(web.sessionId, roomId, options);
        if (table == null) {
            throw new MageException("table not created (the reason came as a SHOW_USERMESSAGE callback, or check gameType/deckType against config.xml)");
        }
        boolean ok = server.roomJoinTable(web.sessionId, roomId, table.getTableId(), name, PlayerType.HUMAN, 1, deck.deck, "");
        if (!ok) {
            server.tableRemove(web.sessionId, roomId, table.getTableId());
            throw new MageException("creator could not take a seat at the new table (see callbacks); table removed");
        }
        logger.info("Web door: " + name + " created table " + table.getTableId() + " (" + gameType + ", " + seats + " seats)");
        web.send(Frames.joined(table.getTableId(), playerId(table.getTableId(), name), name, id));
        warn(web, deck);
    }

    private void join(WebSession web, JsonObject frame, JsonElement id) throws Exception {
        UUID tableId = UUID.fromString(Frames.requireString(frame, "tableId"));
        String name = Frames.requireString(frame, "name");
        PlayerType playerType = (PlayerType) Wire.enumValue(PlayerType.class, Frames.optString(frame, "playerType", PlayerType.HUMAN.name()));
        int skill = Frames.optInt(frame, "skill", 2);
        String password = Frames.optString(frame, "password", "");
        DeckText.Parsed deck = DeckText.parse(Frames.requireString(frame, "deck"));

        if (playerType == PlayerType.HUMAN) {
            ensureConnected(web, name);
        } else if (session(web).getUserId() == null) {
            throw new IllegalStateException("an AI seat is added by a connected user: create the table (or join as Human) first");
        }
        boolean ok = server.roomJoinTable(web.sessionId, roomId(), tableId, name, playerType, skill, deck.deck, password);
        if (!ok) {
            throw new MageException("join refused for " + name + " at table " + tableId + " (no free seat of type " + playerType + ", bad password, or see callbacks)");
        }
        logger.info("Web door: " + name + " (" + playerType + ") joined table " + tableId);
        web.send(Frames.joined(tableId, playerId(tableId, name), name, id));
        warn(web, deck);
    }

    private void start(WebSession web, JsonObject frame, JsonElement id) throws Exception {
        UUID tableId = UUID.fromString(Frames.requireString(frame, "tableId"));
        boolean ok = server.matchStart(web.sessionId, roomId(), tableId);
        if (!ok) {
            throw new MageException("start refused for table " + tableId + " (unknown table, not the owner, or empty seats)");
        }
        logger.info("Web door: table " + tableId + " starting");
        web.send(Frames.result("table.start", true, id));
    }

    private void warn(WebSession web, DeckText.Parsed deck) {
        if (deck.warnings != null) {
            web.send(Frames.error("deck imported with warnings: " + deck.warnings, null));
        }
    }

    /**
     * The player id of the seat holding {@code name}, known as soon as the seat is taken
     * (it is the id GameView.players[].playerId and myPlayerId will carry).
     */
    private UUID playerId(UUID tableId, String name) {
        Table table = managerFactory.tableManager().getTable(tableId);
        if (table == null) {
            return null;
        }
        for (Seat seat : table.getSeats()) {
            if (seat.getPlayer() != null && name.equals(seat.getPlayer().getName())) {
                return seat.getPlayer().getId();
            }
        }
        return null;
    }
}
