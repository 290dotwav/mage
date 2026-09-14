package mage.server.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mage.interfaces.callback.ClientCallback;
import mage.interfaces.callback.ClientCallbackMethod;
import mage.server.DisconnectReason;
import mage.server.Main;
import mage.server.managers.ManagerFactory;
import mage.view.TableClientMessage;
import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.jboss.remoting.callback.AsynchInvokerCallbackHandler;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One browser socket = one server {@link mage.server.Session}.
 * <p>
 * Their Session only needs a JBoss {@code AsynchInvokerCallbackHandler} to push
 * {@link ClientCallback}s to a client (Session.fireCallback -> handleCallbackOneway). This
 * class is that handler for a WebSocket, registered with
 * {@code SessionManager.createSession(sessionId, handler)} exactly like a JBoss connection is
 * from Main.MageServerInvocationHandler.addListener - so Session.java is untouched.
 * <p>
 * Inbound frames of a socket run on one thread per session, in order, off the WebSocket
 * decoder thread (a MageServer call may take a moment: deck loading, table creation).
 */
final class WebSession implements AsynchInvokerCallbackHandler {

    private static final Logger logger = Logger.getLogger(WebSession.class);

    final String sessionId = "web-" + UUID.randomUUID();

    private final WebSocket conn;
    private final ManagerFactory managerFactory;
    private final Wire wire;
    private final TableOps tables;
    private final ExecutorService inbound;

    WebSession(WebSocket conn, ManagerFactory managerFactory, Wire wire, TableOps tables) {
        this.conn = conn;
        this.managerFactory = managerFactory;
        this.wire = wire;
        this.tables = tables;
        this.inbound = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "web-door " + sessionId);
            t.setDaemon(true);
            return t;
        });
    }

    void open() {
        managerFactory.sessionManager().createSession(sessionId, this);
        send(Frames.hello(sessionId, Main.getVersion().toString(), Main.isTestMode(), wire.methodNames()));
    }

    void close() {
        inbound.shutdownNow();
        // same path as a lost JBoss connection: the user keeps his tables for a while and may
        // reconnect by name. If the user already moved to another socket (reconnected before this
        // one noticed it was gone), do not report a lost connection on his behalf.
        boolean userStillHere = managerFactory.sessionManager().getUser(sessionId)
                .map(user -> sessionId.equals(user.getSessionId()))
                .orElse(false);
        managerFactory.sessionManager().disconnect(sessionId, DisconnectReason.LostConnection, userStillHere);
    }

    void receive(String text) {
        inbound.execute(() -> handle(text));
    }

    /**
     * The browser answered a ping: the user is here. Same effect as their client's
     * MageServer.ping (User.updateLastActivity), false until the socket has a user.
     */
    boolean pong() {
        return managerFactory.sessionManager().extendUserSession(sessionId, null);
    }

    private void handle(String text) {
        JsonElement id = null;
        String what = "frame";
        try {
            JsonObject frame = JsonParser.parseString(text).getAsJsonObject();
            id = frame.get("id");
            String kind = Frames.optString(frame, "kind", "");
            switch (kind) {
                case "call": {
                    String method = Frames.requireString(frame, "method");
                    what = "call " + method;
                    JsonElement a = frame.get("args");
                    JsonArray args = a != null && a.isJsonArray() ? a.getAsJsonArray() : new JsonArray();
                    Object result = wire.invoke(method, frame.get("gameId"), args, sessionId);
                    send(Frames.result(method, result, id));
                    break;
                }
                case "table":
                    what = "table " + Frames.optString(frame, "op", "?");
                    tables.handle(this, frame, id);
                    break;
                default:
                    throw new IllegalArgumentException("unknown frame kind '" + kind + "' (call or table)");
            }
        } catch (Throwable ex) {
            String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            logger.warn("Web door: " + sessionId + " " + what + " failed: " + message);
            if (logger.isDebugEnabled()) {
                logger.debug("Web door: failing frame was " + text, ex);
            }
            send(Frames.error(what + ": " + message, id));
        }
    }

    void send(String json) {
        try {
            if (conn.isOpen()) {
                conn.send(json);
            }
        } catch (Exception ex) {
            logger.warn("Web door: " + sessionId + " send failed: " + ex);
        }
    }

    // --- server -> browser: the JBoss handler contract Session.fireCallback relies on ---

    @Override
    public void handleCallbackOneway(Callback callback, boolean serverSide) throws HandleCallbackException {
        deliver(callback);
    }

    @Override
    public void handleCallbackOneway(Callback callback) throws HandleCallbackException {
        deliver(callback);
    }

    @Override
    public void handleCallback(Callback callback, boolean asynch, boolean serverSide) throws HandleCallbackException {
        deliver(callback);
    }

    @Override
    public void handleCallback(Callback callback) throws HandleCallbackException {
        deliver(callback);
    }

    private void deliver(Callback callback) throws HandleCallbackException {
        if (!conn.isOpen()) {
            // Session marks itself invalid and disconnects on this, as for a lost JBoss client
            throw new HandleCallbackException("web socket closed");
        }
        Object payload = callback.getCallbackObject();
        if (!(payload instanceof ClientCallback)) {
            logger.warn("Web door: unexpected callback payload " + (payload == null ? "null" : payload.getClass().getName()));
            return;
        }
        ClientCallback call = (ClientCallback) payload;
        String json;
        // START_GAME is followed by a "decks" frame (Decks): every seat's deck as printings, so
        // the site can fetch every picture of the game before its first view (GAME_INIT) shows.
        String decks = null;
        try {
            // data is compressed (ClientCallback.setData) and the object may be shared between users
            // (ChatSession broadcasts one instance), so decompress a private copy, never the original
            ClientCallback copy = copy(call);
            copy.decompressData();
            JsonObject frame = Frames.callback(copy, copy.getData());
            if (copy.getMethod().name().startsWith("GAME_")) {
                StackControllers.enrich(frame, copy.getObjectId(), managerFactory);
                Commanders.enrich(frame, copy.getObjectId(), managerFactory);
            }
            json = Frames.GSON.toJson(frame);
            if (copy.getMethod() == ClientCallbackMethod.START_GAME && copy.getData() instanceof TableClientMessage) {
                decks = decksFrame((TableClientMessage) copy.getData(), copy.getObjectId());
            }
        } catch (Throwable ex) {
            // never guess a question: name the callback the door could not forward
            logger.error("Web door: cannot forward " + call.getInfo() + " to " + sessionId, ex);
            json = Frames.error("cannot forward callback " + call.getMethod() + " (messageId " + call.getMessageId() + "): " + ex, null);
        }
        try {
            conn.send(json);
            if (decks != null) {
                conn.send(decks);
            }
        } catch (Exception ex) {
            throw new HandleCallbackException("web socket send failed: " + ex.getMessage(), ex);
        }
    }

    /** The decks frame for a game that starts, or null (and a log line) when the table cannot be read: the game goes on without it. */
    private String decksFrame(TableClientMessage started, UUID gameId) {
        try {
            String frame = Decks.frame(started, gameId, managerFactory);
            if (frame == null) {
                logger.warn("Web door: no decks frame for game " + gameId + " to " + sessionId + " (table " + started.getCurrentTableId() + " not found)");
            }
            return frame;
        } catch (RuntimeException ex) {
            logger.warn("Web door: decks frame for game " + gameId + " to " + sessionId + " failed: " + ex, ex);
            return null;
        }
    }

    private static ClientCallback copy(ClientCallback call) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(call);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (ClientCallback) in.readObject();
        }
    }
}
