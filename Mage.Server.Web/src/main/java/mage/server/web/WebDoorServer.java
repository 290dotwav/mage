package mage.server.web;

import mage.server.MageServerImpl;
import mage.server.managers.ManagerFactory;
import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The WebSocket listener: one {@link WebSession} per connection, text frames only.
 * <p>
 * Keepalive: the server pings every socket every {@link #PING_SECONDS} seconds (Java-WebSocket's
 * connection-lost timer; a browser answers with a pong on its own, no script involved) and each
 * pong refreshes the user's last activity on the server. Their UserManagerImpl checks every 30 s
 * and tells the opponents "&lt;name&gt; catch connection problems for N secs" about any user whose
 * last activity is older than 30 s - and only their own client's MageServer.ping and the
 * sendPlayer* answers refresh it, so a browser idle while the bots think was called lagging.
 * A socket that stops answering pongs is closed after 1.5 x PING_SECONDS, which is a lost
 * connection like any other (the user keeps his seat for 3 minutes and may reconnect by name).
 */
final class WebDoorServer extends WebSocketServer {

    private static final Logger logger = Logger.getLogger(WebDoorServer.class);

    /** -Dxmage.web.pingSeconds: must stay under UserManagerImpl's 30 s inform threshold. */
    static final int PING_SECONDS = Integer.getInteger("xmage.web.pingSeconds", 15);

    private final ManagerFactory managerFactory;
    private final Wire wire;
    private final TableOps tables;
    private final Map<WebSocket, WebSession> sessions = new ConcurrentHashMap<>();

    WebDoorServer(InetSocketAddress address, ManagerFactory managerFactory, MageServerImpl server) {
        super(address);
        this.managerFactory = managerFactory;
        this.wire = new Wire(server);
        this.tables = new TableOps(managerFactory, server);
        setReuseAddr(true);
        setConnectionLostTimeout(PING_SECONDS);
    }

    @Override
    public void onStart() {
        logger.info("Web door listening on ws://" + getAddress().getHostString() + ":" + getPort()
                + " (ping every " + PING_SECONDS + " s)");
    }

    @Override
    public void onWebsocketPong(WebSocket conn, Framedata f) {
        WebSession session = sessions.get(conn);
        if (session != null) {
            session.pong();
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        WebSession session = new WebSession(conn, managerFactory, wire, tables);
        sessions.put(conn, session);
        session.open();
        logger.info("Web door: " + session.sessionId + " opened from " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        WebSession session = sessions.remove(conn);
        if (session != null) {
            session.close();
            logger.info("Web door: " + session.sessionId + " closed (" + code + (reason == null || reason.isEmpty() ? "" : " " + reason) + ")");
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        WebSession session = sessions.get(conn);
        if (session != null) {
            session.receive(message);
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        WebSession session = sessions.get(conn);
        if (session != null) {
            session.send(Frames.error("binary frames are not accepted, send JSON text", null));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        WebSession session = conn == null ? null : sessions.get(conn);
        logger.warn("Web door: error on " + (session == null ? "listener" : session.sessionId) + ": " + ex, ex);
    }
}
