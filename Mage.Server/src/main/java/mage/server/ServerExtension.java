package mage.server;

import mage.server.managers.ManagerFactory;

/**
 * Optional server extension, discovered with {@link java.util.ServiceLoader} once the
 * server is started (see {@link Main}). An extension jar on the server classpath
 * declares its implementation in META-INF/services/mage.server.ServerExtension.
 * <p>
 * Example: Mage.Server.Web adds a WebSocket/JSON door for browser clients.
 */
public interface ServerExtension {

    void onServerStarted(ManagerFactory managerFactory, MageServerImpl server);
}
