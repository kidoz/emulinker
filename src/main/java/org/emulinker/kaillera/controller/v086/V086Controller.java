package org.emulinker.kaillera.controller.v086;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.emulinker.config.ControllersConfig;
import org.emulinker.config.ServerConfig;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.messaging.ParseException;
import org.emulinker.kaillera.controller.v086.action.ActionRouter;
import org.emulinker.kaillera.controller.v086.action.FatalActionException;
import org.emulinker.kaillera.controller.v086.action.V086Action;
import org.emulinker.kaillera.controller.v086.action.V086GameEventHandler;
import org.emulinker.kaillera.controller.v086.action.V086ServerEventHandler;
import org.emulinker.kaillera.controller.v086.action.V086UserEventHandler;
import org.emulinker.kaillera.controller.v086.protocol.V086Bundle;
import org.emulinker.kaillera.controller.v086.protocol.V086BundleFormatException;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.event.KailleraEvent;
import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.event.ServerEvent;
import org.emulinker.kaillera.model.event.UserEvent;
import org.emulinker.kaillera.model.exception.NewConnectionException;
import org.emulinker.kaillera.model.exception.ServerFullException;
import org.emulinker.net.BindException;
import org.emulinker.net.PrivateUDPServer;
import org.emulinker.util.ClientGameDataCache;
import org.emulinker.util.EmuLinkerExecutor;
import org.emulinker.util.EmuUtil;
import org.emulinker.util.GameDataCache;
import org.emulinker.util.ServerGameDataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class V086Controller implements KailleraServerController {
    private static final Logger log = LoggerFactory.getLogger(V086Controller.class);

    private static final int MAX_BUNDLE_SIZE = 5;

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

        V086ClientHandler clientHandler = new V086ClientHandler(clientSocketAddress);
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

    public final class V086ClientHandler extends PrivateUDPServer implements KailleraEventListener {
        private volatile KailleraUser user;
        private int messageNumberCounter = 0;
        private int prevMessageNumber = -1;
        private int lastMessageNumber = -1;
        private GameDataCache clientCache = null;
        private GameDataCache serverCache = null;

        // private LinkedList<V086Message> lastMessages = new LinkedList<V086Message>();
        private LastMessageBuffer lastMessageBuffer = new LastMessageBuffer(MAX_BUNDLE_SIZE);
        private V086Message[] outMessages = new V086Message[MAX_BUNDLE_SIZE];

        private ByteBuffer inBuffer = ByteBuffer.allocateDirect(bufferSize);
        private ByteBuffer outBuffer = ByteBuffer.allocateDirect(bufferSize);

        private Object inSynch = new Object();
        private Object outSynch = new Object();

        private long testStart;
        private long lastMeasurement;
        private int measurementCount = 0;
        private int bestTime = Integer.MAX_VALUE;

        private int clientRetryCount = 0;
        private long lastResend = 0;

        private V086ClientHandler(InetSocketAddress remoteSocketAddress) {
            super(false, remoteSocketAddress.getAddress());

            inBuffer.order(ByteOrder.LITTLE_ENDIAN);
            outBuffer.order(ByteOrder.LITTLE_ENDIAN);

            resetGameDataCache();
        }

        public String toString() {
            if (getBindPort() > 0)
                return "V086Controller(" + getBindPort() + ")";
            else
                return "V086Controller(unbound)";
        }

        public V086Controller getController() {
            return V086Controller.this;
        }

        public KailleraUser getUser() {
            return user;
        }

        public synchronized int getNextMessageNumber() {
            if (messageNumberCounter > 0xFFFF)
                messageNumberCounter = 0;

            return messageNumberCounter++;
        }

        /*
         * public List<V086Message> getLastMessage() { return lastMessages; }
         */

        public int getPrevMessageNumber() {
            return prevMessageNumber;
        }

        public int getLastMessageNumber() {
            return lastMessageNumber;
        }

        public GameDataCache getClientGameDataCache() {
            return clientCache;
        }

        public GameDataCache getServerGameDataCache() {
            return serverCache;
        }

        public void resetGameDataCache() {
            clientCache = new ClientGameDataCache(256);
            serverCache = new ServerGameDataCache(256);
        }

        public void startSpeedTest() {
            long now = System.currentTimeMillis();
            testStart = now;
            lastMeasurement = now;
            measurementCount = 0;
        }

        public void addSpeedMeasurement() {
            int et = (int) (System.currentTimeMillis() - lastMeasurement);
            if (et < bestTime)
                bestTime = et;
            measurementCount++;
            lastMeasurement = System.currentTimeMillis();
        }

        public int getSpeedMeasurementCount() {
            return measurementCount;
        }

        public int getBestNetworkSpeed() {
            return bestTime;
        }

        public int getAverageNetworkSpeed() {
            if (measurementCount <= 0) {
                return 0;
            }
            return (int) ((lastMeasurement - testStart) / measurementCount);
        }

        public void bind(int port) throws BindException {
            super.bind(port);
        }

        public void start(KailleraUser user) {
            this.user = user;
            log.debug(toString() + " thread starting (ThreadPool:" + threadPool.getActiveCount()
                    + "/" + threadPool.getPoolSize() + ")");
            threadPool.execute(this);

            /*
             * long s = System.currentTimeMillis(); while (!isBound() &&
             * (System.currentTimeMillis() - s) < 1000) { try { Thread.sleep(100); } catch
             * (Exception e) { log.error("Sleep Interrupted!", e); } }
             *
             * if (!isBound()) {
             * log.error("V086ClientHandler failed to start for client from " +
             * getRemoteInetAddress().getHostAddress()); return; }
             */

            log.debug(toString() + " thread started (ThreadPool:" + threadPool.getActiveCount()
                    + "/" + threadPool.getPoolSize() + ")");
            clientHandlers.put(user.getID(), this);
        }

        public void stop() {
            synchronized (this) {
                if (getStopFlag())
                    return;

                int port = -1;
                if (isBound())
                    port = getBindPort();
                log.debug(this.toString() + " Stopping!");
                super.stop();

                if (port > 0) {
                    log.debug(toString() + " returning port " + port + " to available port queue: "
                            + (portRangeQueue.size() + 1) + " available");
                    portRangeQueue.add(port);
                }
            }

            KailleraUser localUser = user;
            if (localUser != null) {
                clientHandlers.remove(localUser.getID());
                localUser.stop();
                user = null;
            }
        }

        protected ByteBuffer getBuffer() {
            // return ByteBufferMessage.getBuffer(bufferSize);
            inBuffer.clear();
            return inBuffer;
        }

        protected void releaseBuffer(ByteBuffer buffer) {
            // ByteBufferMessage.releaseBuffer(buffer);
            // buffer.clear();
        }

        protected void handleReceived(ByteBuffer buffer) {
            V086Bundle inBundle = null;

            try {
                inBundle = V086Bundle.parse(buffer, lastMessageNumber);
                // inBundle = V086Bundle.parse(buffer, -1);
            } catch (ParseException e) {
                buffer.rewind();
                log.warn(toString() + " failed to parse: " + EmuUtil.dumpBuffer(buffer), e);
                return;
            } catch (V086BundleFormatException e) {
                buffer.rewind();
                log.warn(toString() + " received invalid message bundle: "
                        + EmuUtil.dumpBuffer(buffer), e);
                return;
            } catch (MessageFormatException e) {
                buffer.rewind();
                log.warn(toString() + " received invalid message: " + EmuUtil.dumpBuffer(buffer),
                        e);
                return;
            }

            // log.debug("-> " + inBundle);

            if (inBundle.getNumMessages() == 0) {
                log.debug(toString() + " received bundle of " + inBundle.getNumMessages()
                        + " messages from " + user);
                clientRetryCount++;
                resend(clientRetryCount);
                return;
            } else {
                clientRetryCount = 0;
            }

            try {
                synchronized (inSynch) {
                    // read the bundle from back to front to process the oldest messages first
                    V086Message[] messages = inBundle.getMessages();
                    for (int i = (inBundle.getNumMessages() - 1); i >= 0; i--) {
                        if (messages[i].getNumber() > lastMessageNumber) {
                            prevMessageNumber = lastMessageNumber;
                            lastMessageNumber = messages[i].getNumber();

                            if ((prevMessageNumber + 1) != lastMessageNumber) {
                                log.warn(user + " dropped a packet! (" + prevMessageNumber + " to "
                                        + lastMessageNumber + ")");
                                user.droppedPacket();
                            }

                            V086Action action = actionRouter.getAction(messages[i].getID());
                            if (action == null) {
                                log.error("No action defined to handle client message: "
                                        + messages[i]);
                                continue;
                            }

                            // log.debug(user + " -> " + message);
                            action.performAction(messages[i], this);
                        }
                    }
                }
            } catch (FatalActionException e) {
                log.warn(toString() + " fatal action, closing connection: " + e.getMessage());
                stop();
            }
        }

        public void actionPerformed(KailleraEvent event) {
            if (event instanceof GameEvent gameEvent) {
                V086GameEventHandler eventHandler = actionRouter
                        .getGameEventHandler(event.getClass());
                if (eventHandler == null) {
                    log.error(toString()
                            + " found no GameEventHandler registered to handle game event: "
                            + event);
                    return;
                }

                eventHandler.handleEvent(gameEvent, this);
            } else if (event instanceof ServerEvent serverEvent) {
                V086ServerEventHandler eventHandler = actionRouter
                        .getServerEventHandler(event.getClass());
                if (eventHandler == null) {
                    log.error(toString()
                            + " found no ServerEventHandler registered to handle server event: "
                            + event);
                    return;
                }

                eventHandler.handleEvent(serverEvent, this);
            } else if (event instanceof UserEvent userEvent) {
                V086UserEventHandler eventHandler = actionRouter
                        .getUserEventHandler(event.getClass());
                if (eventHandler == null) {
                    log.error(toString()
                            + " found no UserEventHandler registered to handle user event: "
                            + event);
                    return;
                }

                eventHandler.handleEvent(userEvent, this);
            }
        }

        public void resend(int timeoutCounter) {
            synchronized (outSynch) {
                // if ((System.currentTimeMillis() - lastResend) > 1000)
                // if ((System.currentTimeMillis() - lastResend) > (user.getPing()*3))
                if ((System.currentTimeMillis() - lastResend) > server.getMaxPing()) {
                    // int numToSend = (3+timeoutCounter);
                    int numToSend = (3 * timeoutCounter);
                    if (numToSend > MAX_BUNDLE_SIZE)
                        numToSend = MAX_BUNDLE_SIZE;
                    log.debug(this + ": resending last " + numToSend + " messages");
                    send(null, numToSend);
                    lastResend = System.currentTimeMillis();
                } else {
                    log.debug("Skipping resend...");
                }
            }
        }

        public void send(V086Message outMessage) {
            send(outMessage, 3);
        }

        public void send(V086Message outMessage, int numToSend) {
            synchronized (outSynch) {
                if (outMessage != null)
                    lastMessageBuffer.add(outMessage);

                numToSend = lastMessageBuffer.fill(outMessages, numToSend);
                V086Bundle outBundle = new V086Bundle(outMessages, numToSend);
                // log.debug("<- " + outBundle);
                outBundle.writeTo(outBuffer);
                outBuffer.flip();
                super.send(outBuffer);
                outBuffer.clear();
            }
        }
    }
}
