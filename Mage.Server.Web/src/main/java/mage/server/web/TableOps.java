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
import mage.players.net.SkipPrioritySteps;
import mage.players.net.UserData;
import mage.server.DisconnectReason;
import mage.server.MageServerImpl;
import mage.server.Main;
import mage.server.Session;
import mage.server.User;
import mage.server.managers.ManagerFactory;
import mage.view.TableView;
import org.apache.log4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * "table" frames, the three operations a browser needs before a game exists:
 * <pre>
 * { kind: "table", op: "create", gameType: "Commander Free For All", seats: 4, deck: "&lt;.dck text&gt;", name: "Alice",
 *   seatTypes?: ["Human","Computer - mad","Computer - mad","Computer - mad"], tableName?: "FG", deckType?: "Variant Magic - Commander",
 *   mulliganType?: "LONDON" | "TEN" | ... (a MulliganType name; default LONDON) }
 * { kind: "table", op: "join",   tableId, deck, name, playerType?: "Human" | "Computer - mad", skill?: 2, thinkSeconds?: 3, password?: "" }
 *   - a name already seated at tableId re-seats (reconnection): no new seat, deck not needed
 *   - thinkSeconds caps an AI seat's simulation time per decision (ComputerPlayer6: skill * 3 s
 *     by default, 6 s at skill 2); -Dxmage.web.aiThinkSeconds gives the default when the frame has none
 * { kind: "table", op: "start",  tableId }
 * { kind: "table", op: "watch",  tableId, name }
 *   - sits nobody down: the socket connects under "name" and asks to watch the game running at
 *     that table (roomWatchTable). The server answers with a WATCHGAME callback carrying the
 *     game id, and the browser then calls gameWatchStart(gameId) as their own client does.
 *     Refused for a table that is not DUELING, and for a name already playing at it.
 * </pre>
 * "name" is the seat's player name. On a socket that has not connected a user yet, create
 * and a human join connect one under that name first (anonymous mode, as their own client
 * does with connectUser + connectSetUserData). Seats are typed when the table is created
 * (Table.getNextAvailableSeat matches the PlayerType), hence seatTypes; default all Human.
 * create also seats its creator with the given deck, like their NewTableDialog does.
 */
final class TableOps {

    private static final Logger logger = Logger.getLogger(TableOps.class);

    /** Per-decision simulation cap for AI seats when the join frame has no thinkSeconds; 0 = the AI's own (skill * 3 s). */
    static final int DEFAULT_AI_THINK_SECONDS = Integer.getInteger("xmage.web.aiThinkSeconds", 0);
    static final int MAX_AI_THINK_SECONDS = 600;

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
            case "watch":
                watch(session, frame, id);
                break;
            default:
                throw new IllegalArgumentException("unknown table op '" + op + "' (create, join, start, watch)");
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

    /**
     * Connect the socket's session to the user {@code name} (anonymous mode). This is their
     * own reconnection path: Session.connectUserHandling finds an existing user of that name
     * (kept 180 s after a lost connection), moves him to this session and replays his tables
     * and games through User.onReconnect (JOINED_TABLE, START_GAME, GAME_INIT, open question).
     */
    private void ensureConnected(WebSession web, String name) throws MageException {
        Session session = session(web);
        if (session.getUserId() != null) {
            User user = managerFactory.userManager().getUser(session.getUserId()).orElse(null);
            if (user != null && !name.equals(user.getName())) {
                throw new IllegalStateException("this socket is connected as '" + user.getName() + "': a human seat uses that name");
            }
            return;
        }
        if (!server.connectUser(name, "", web.sessionId, "", Main.getVersion(), "web")) {
            throw new MageException("connect refused for user '" + name + "' (the reason came as a SHOW_USERMESSAGE callback)");
        }
        server.connectSetUserData(web.sessionId, webUserData(), Main.getVersion().toString(), "web");
        logger.info("Web door: " + web.sessionId + " connected as " + name);
    }

    /**
     * The seat's preferences, as the desktop client would send them — except that
     * every step stops. The default {@link mage.players.net.SkipPrioritySteps}
     * stops at the two main phases only, so during another player's turn the
     * web seat was never asked at upkeep, draw, combat or end: the whole turn
     * went by in one frame against instant bots (the site's owner: « mon
     * adversaire fait presque ses tours en instantané »). The site paces every
     * step it is asked about (0.7 s when there is nothing to do), so the one
     * thing the door has to do is make sure it is asked.
     */
    static UserData webUserData() {
        UserData data = UserData.getDefaultUserDataView();
        for (SkipPrioritySteps steps : new SkipPrioritySteps[]{data.getUserSkipPrioritySteps().getYourTurn(), data.getUserSkipPrioritySteps().getOpponentTurn()}) {
            steps.setUpkeep(true);
            steps.setDraw(true);
            steps.setMain1(true);
            steps.setBeforeCombat(true);
            steps.setEndOfCombat(true);
            steps.setMain2(true);
            steps.setEndOfTurn(true);
        }
        // Several triggers of the SAME ability, with the same rule text and the
        // same targets, are put on the stack in any order without asking:
        // `HumanPlayer.chooseTriggeredAbility` honours this flag and orders
        // them itself. The site's owner, with a board full of one trigger
        // repeated: « quand plusieurs triggers identiques sont mis sur la
        // stack, il faut pouvoir valider l'ordre d'un coup ». For identical
        // ones there is no order to validate, so nothing is asked at all; a
        // list that really differs is still the player's to order.
        data.setAutoOrderTrigger(true);
        return data;
    }

    /**
     * A name that a previous socket left seated (the server keeps a user 180 s after his connection
     * dropped, tables and games included) and that now asks for a NEW table: his old tables are left
     * and his old games conceded first. Otherwise connectUser (ensureConnected) finds that user,
     * calls it a reconnection, and User.onReconnect replays the OLD table, the OLD game and its open
     * question to this socket — the browser then shows last game's "Select a noncreature artifact"
     * over this game's opening hand. A join by that name to a table he already sits at is the
     * reconnection it looks like and keeps everything ({@code keepTableId}); on a socket that already
     * has a user nothing is done (a second create on one socket is the server's business).
     */
    private void leaveOldTables(WebSession web, String name, UUID keepTableId) {
        Session session = session(web);
        if (session.getUserId() != null) {
            return;
        }
        User user = managerFactory.userManager().getUserByName(name).orElse(null);
        if (user == null) {
            return;
        }
        if (keepTableId != null && playerId(keepTableId, name) != null) {
            return;
        }
        logger.info("Web door: " + name + " asks for a new table while still seated from an earlier connection: leaving the old tables");
        user.removeUserFromAllTables(DisconnectReason.DisconnectedByUserButKeepTables);
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

    /**
     * A MulliganType by enum name ("LONDON", "TEN", "VANCOUVER"...) or display name ("Mulligan à 10").
     */
    static MulliganType mulliganType(String text) {
        String key = text.trim().toUpperCase(Locale.ENGLISH).replace(' ', '_');
        for (MulliganType type : MulliganType.values()) {
            if (type.name().equals(key)) {
                return type;
            }
        }
        MulliganType byDisplayName = MulliganType.valueByName(text.trim());
        if (byDisplayName != MulliganType.GAME_DEFAULT) {
            return byDisplayName;
        }
        List<String> names = new ArrayList<>();
        for (MulliganType type : MulliganType.values()) {
            names.add(type.name());
        }
        throw new IllegalArgumentException("unknown mulliganType '" + text + "' (one of " + names + ")");
    }

    private void create(WebSession web, JsonObject frame, JsonElement id) throws Exception {
        String gameType = Frames.optString(frame, "gameType", "Commander Free For All");
        int seats = Frames.optInt(frame, "seats", 4);
        String name = Frames.requireString(frame, "name");
        String tableName = Frames.optString(frame, "tableName", name);
        String deckType = Frames.optString(frame, "deckType", defaultDeckType(gameType));
        MulliganType mulliganType = mulliganType(Frames.optString(frame, "mulliganType", MulliganType.LONDON.name()));
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

        leaveOldTables(web, name, null);
        ensureConnected(web, name);

        MatchOptions options = new MatchOptions(tableName, gameType, false);
        options.getPlayerTypes().addAll(seatTypes);
        options.setDeckType(deckType);
        options.setLimited(false);
        options.setAttackOption(MultiplayerAttackOption.MULTIPLE);
        options.setRange(RangeOfInfluence.ALL);
        options.setWinsNeeded(1);
        options.setFreeMulligans(0);
        options.setMullgianType(mulliganType);
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
        logger.info("Web door: " + name + " created table " + table.getTableId() + " (" + gameType + ", " + seats + " seats, mulligan " + mulliganType.name() + ")");
        web.send(Frames.joined(table.getTableId(), playerId(table.getTableId(), name), name, false, deck, id));
    }

    private void join(WebSession web, JsonObject frame, JsonElement id) throws Exception {
        UUID tableId = UUID.fromString(Frames.requireString(frame, "tableId"));
        String name = Frames.requireString(frame, "name");
        PlayerType playerType = (PlayerType) Wire.enumValue(PlayerType.class, Frames.optString(frame, "playerType", PlayerType.HUMAN.name()));
        int skill = Frames.optInt(frame, "skill", 2);
        int thinkSeconds = Frames.optInt(frame, "thinkSeconds", DEFAULT_AI_THINK_SECONDS);
        if (thinkSeconds < 0 || thinkSeconds > MAX_AI_THINK_SECONDS) {
            throw new IllegalArgumentException("thinkSeconds must be 1.." + MAX_AI_THINK_SECONDS + " (0 or absent: the AI's own skill * 3)");
        }
        String password = Frames.optString(frame, "password", "");

        if (playerType == PlayerType.HUMAN) {
            leaveOldTables(web, name, tableId);
            ensureConnected(web, name);
            UUID seated = playerId(tableId, name);
            if (seated != null) {
                // already at this table: a reconnection, not a second seat. onReconnect (started by
                // connectUser above) replays the table and game to this socket; nothing to join.
                logger.info("Web door: " + name + " re-seated at table " + tableId + " (reconnection)");
                web.send(Frames.joined(tableId, seated, name, true, null, id));
                return;
            }
        } else if (session(web).getUserId() == null) {
            throw new IllegalStateException("an AI seat is added by a connected user: create the table (or join as Human) first");
        }
        DeckText.Parsed deck = DeckText.parse(Frames.requireString(frame, "deck"));
        boolean ok = server.roomJoinTable(web.sessionId, roomId(), tableId, name, playerType, skill, deck.deck, password);
        if (!ok) {
            throw new MageException("join refused for " + name + " at table " + tableId + " (no free seat of type " + playerType + ", bad password, or see callbacks)");
        }
        logger.info("Web door: " + name + " (" + playerType + ") joined table " + tableId);
        if (playerType != PlayerType.HUMAN && thinkSeconds > 0) {
            setThinkTime(tableId, name, thinkSeconds);
        }
        web.send(Frames.joined(tableId, playerId(tableId, name), name, false, deck, id));
    }

    /**
     * Cap the simulation time of the AI seated as {@code name}: ComputerPlayer6 (the "mad" AI,
     * also the base of every AI type that simulates) exposes setMaxThinkTimeSecs(int) and gives
     * skill * 3 seconds to each decision (main phases, attackers, blockers - on every player's
     * turn) by default. The AI classes live in a plugin classloader, hence reflection. The seat's
     * Player object is the one the game will use (TableController -> MatchImpl.addPlayer ->
     * GameImpl.addPlayer keep the instance), so setting it here at join time is enough. An AI
     * without that method (none today) just keeps its own timing.
     */
    private void setThinkTime(UUID tableId, String name, int thinkSeconds) {
        Table table = managerFactory.tableManager().getTable(tableId);
        if (table == null) {
            return;
        }
        for (Seat seat : table.getSeats()) {
            if (seat.getPlayer() == null || !name.equals(seat.getPlayer().getName())) {
                continue;
            }
            try {
                Method setter = seat.getPlayer().getClass().getMethod("setMaxThinkTimeSecs", int.class);
                setter.invoke(seat.getPlayer(), thinkSeconds);
                logger.info("Web door: " + name + " thinks at most " + thinkSeconds + " s per decision");
            } catch (NoSuchMethodException ex) {
                logger.warn("Web door: " + name + " is a " + seat.getPlayer().getClass().getSimpleName() + " without setMaxThinkTimeSecs: thinkSeconds ignored");
            } catch (ReflectiveOperationException | RuntimeException ex) {
                logger.warn("Web door: could not set thinkSeconds on " + name + ": " + ex);
            }
            return;
        }
    }

    /**
     * Watch a table nobody offered us a chair at: the front door's spectator mode
     * (the owner: « sinon je suis en mode spectateur et je ne peux rien faire,
     * uniquement voir ce que voit un adversaire »).
     * <p>
     * Their own client does exactly this — {@code roomWatchTable}, which for a
     * running match calls {@code User.ccWatchGame} and so sends this socket a
     * WATCHGAME callback with the game id; the browser answers it with
     * {@code gameWatchStart(gameId)} and from then on receives the game's views
     * as a watcher: the board, the log, nobody's hand, and no question ever.
     * <p>
     * Refused by their {@code TableController.watchTable} when the table is not
     * DUELING or when this user is playing at it, which is the rule we want: a
     * player at the table plays it, he does not watch it.
     */
    private void watch(WebSession web, JsonObject frame, JsonElement id) throws Exception {
        UUID tableId = UUID.fromString(Frames.requireString(frame, "tableId"));
        String name = Frames.requireString(frame, "name");
        ensureConnected(web, name);
        boolean ok = server.roomWatchTable(web.sessionId, roomId(), tableId);
        if (!ok) {
            throw new MageException("watch refused for table " + tableId + " (no game running there, you are seated at it, or watching is not allowed)");
        }
        logger.info("Web door: " + name + " is watching table " + tableId);
        web.send(Frames.result("table.watch", true, id));
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
