package mage.server.web;

import mage.server.MageServerImpl;
import mage.server.ServerExtension;
import mage.server.managers.ManagerFactory;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;

/**
 * Entry point, found by Mage.Server through ServiceLoader
 * (META-INF/services/mage.server.ServerExtension) once the JBoss server is up.
 * <p>
 * Settings (JVM properties of the server launcher):
 * <ul>
 * <li>{@code -Dxmage.web.port=17172} - listening port (default 17172)</li>
 * <li>{@code -Dxmage.web.host=0.0.0.0} - bind address</li>
 * <li>{@code -Dxmage.web.enabled=false} - keep the jar on the classpath but do not listen</li>
 * </ul>
 */
public final class WebDoor implements ServerExtension {

    private static final Logger logger = Logger.getLogger(WebDoor.class);

    public static final int DEFAULT_PORT = 17172;

    private WebDoorServer server;

    @Override
    public void onServerStarted(ManagerFactory managerFactory, MageServerImpl mageServer) {
        if (!Boolean.parseBoolean(System.getProperty("xmage.web.enabled", "true"))) {
            logger.info("Web door disabled by -Dxmage.web.enabled=false");
            return;
        }
        int port = Integer.getInteger("xmage.web.port", DEFAULT_PORT);
        String host = System.getProperty("xmage.web.host", "0.0.0.0");
        server = new WebDoorServer(new InetSocketAddress(host, port), managerFactory, mageServer);
        server.start(); // its own selector thread; onStart logs the bound address
        logger.info("Web door starting on " + host + ":" + port + " (WebSocket, JSON)");
    }
}
