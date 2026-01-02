package org.emulinker.kaillera.controller.v086;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.emulinker.config.ControllersConfig;
import org.emulinker.config.ServerConfig;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.v086.action.ActionRouter;
import org.emulinker.kaillera.controller.v086.action.V086Action;
import org.emulinker.kaillera.controller.v086.action.V086GameEventHandler;
import org.emulinker.kaillera.controller.v086.action.V086ServerEventHandler;
import org.emulinker.kaillera.controller.v086.action.V086UserEventHandler;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.exception.NewConnectionException;
import org.emulinker.kaillera.model.exception.ServerFullException;
import org.emulinker.net.BindException;
import org.emulinker.util.EmuLinkerExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;

public final class V086Controller implements KailleraServerController {
    private static final Logger log = LoggerFactory.getLogger(V086Controller.class);

    private final int bufferSize;
    private volatile boolean isRunning = false;

    private final EmuLinkerExecutor threadPool;
    private final KailleraServer server;
    private final String[] clientTypes;
    private final Map<Integer, V086ClientHandler> clientHandlers = new ConcurrentHashMap<Integer, V086ClientHandler>();

    private final int portRangeStart;
    private final int extraPorts;
    private final Queue<Integer> portRangeQueue = new ConcurrentLinkedQueue<>();

    private final ActionRouter actionRouter;

    public V086Controller(KailleraServer server, EmuLinkerExecutor threadPool,
            ControllersConfig controllersConfig, ServerConfig serverConfig,
            ActionRouter actionRouter) {
        this.threadPool = threadPool;
        this.server = server;
        this.actionRouter = actionRouter;

        ControllersConfig.V086 v086Config = controllersConfig.getV086();
        this.bufferSize = v086Config.getBufferSize();
        this.clientTypes = v086Config.getClientTypes().toArray(new String[0]);

        this.portRangeStart = v086Config.getPortRangeStart();
        this.extraPorts = v086Config.getExtraPorts();
        int maxPort = 0;
        for (int i = portRangeStart; i <= (portRangeStart + serverConfig.getMaxUsers()
                + extraPorts); i++) {
            portRangeQueue.add(i);
            maxPort = i;
        }

        log.warn("Listening on UDP ports: " + portRangeStart + " to " + maxPort
                + ".  Make sure these ports are open in your firewall!");
    }

    public String getVersion() {
        return "v086";
    }

    public String[] getClientTypes() {
        return clientTypes;
    }

    public KailleraServer getServer() {
        return server;
    }

    public int getNumClients() {
        return clientHandlers.size();
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public Map<Class<?>, V086ServerEventHandler> getServerEventHandlers() {
        return actionRouter.getServerEventHandlers();
    }

    public Map<Class<?>, V086GameEventHandler> getGameEventHandlers() {
        return actionRouter.getGameEventHandlers();
    }

    public Map<Class<?>, V086UserEventHandler> getUserEventHandlers() {
        return actionRouter.getUserEventHandlers();
    }

    public V086Action[] getActions() {
        return actionRouter.getActions();
    }

    public ActionRouter getActionRouter() {
        return actionRouter;
    }

    public final Map<Integer, V086ClientHandler> getClientHandlers() {
        return clientHandlers;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public String toString() {
        return "V086Controller[clients=" + clientHandlers.size() + " isRunning=" + isRunning + "]";
    }

    public int newConnection(InetSocketAddress clientSocketAddress, String protocol)
            throws ServerFullException, NewConnectionException {
        if (!isRunning)
            throw new NewConnectionException("Controller is not running");

        V086ClientHandler clientHandler = new V086ClientHandler(clientSocketAddress, this,
                bufferSize, threadPool, clientHandlers, portRangeQueue, server, actionRouter);
        KailleraUser user = server.newConnection(clientSocketAddress, protocol, clientHandler);

        int boundPort = -1;
        int bindAttempts = 0;
        while (bindAttempts++ < 5) {
            Integer portInteger = portRangeQueue.poll();
            if (portInteger == null) {
                log.error("No ports are available to bind for: " + user);
            } else {
                int port = portInteger.intValue();
                log.info("Private port " + port + " allocated to: " + user);

                try {
                    clientHandler.bind(port);
                    boundPort = port;
                    break;
                } catch (BindException e) {
                    log.error("Failed to bind to port " + port + " for: " + user + ": "
                            + e.getMessage(), e);
                    log.debug(toString() + " returning port " + port + " to available port queue: "
                            + (portRangeQueue.size() + 1) + " available");
                    portRangeQueue.add(port);
                }
            }

            try {
                // pause very briefly to give the OS a chance to free a port
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (boundPort < 0) {
            clientHandler.stop();
            throw new NewConnectionException("Failed to bind!");
        }

        clientHandler.start(user);
        return boundPort;
    }

    public synchronized void start() {
        isRunning = true;
    }

    public synchronized void stop() {
        isRunning = false;

        for (V086ClientHandler clientHandler : clientHandlers.values())
            clientHandler.stop();

        clientHandlers.clear();
    }
}
