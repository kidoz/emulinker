package org.emulinker.kaillera.controller.v086;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.emulinker.config.ControllersConfig;
import org.emulinker.config.ServerConfig;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.messaging.ParseException;
import org.emulinker.kaillera.controller.v086.action.ACKAction;
import org.emulinker.kaillera.controller.v086.action.CachedGameDataAction;
import org.emulinker.kaillera.controller.v086.action.ChatAction;
import org.emulinker.kaillera.controller.v086.action.CloseGameAction;
import org.emulinker.kaillera.controller.v086.action.CreateGameAction;
import org.emulinker.kaillera.controller.v086.action.DropGameAction;
import org.emulinker.kaillera.controller.v086.action.FatalActionException;
import org.emulinker.kaillera.controller.v086.action.GameChatAction;
import org.emulinker.kaillera.controller.v086.action.GameDataAction;
import org.emulinker.kaillera.controller.v086.action.GameDesynchAction;
import org.emulinker.kaillera.controller.v086.action.GameInfoAction;
import org.emulinker.kaillera.controller.v086.action.GameKickAction;
import org.emulinker.kaillera.controller.v086.action.GameStatusAction;
import org.emulinker.kaillera.controller.v086.action.GameTimeoutAction;
import org.emulinker.kaillera.controller.v086.action.InfoMessageAction;
import org.emulinker.kaillera.controller.v086.action.JoinGameAction;
import org.emulinker.kaillera.controller.v086.action.KeepAliveAction;
import org.emulinker.kaillera.controller.v086.action.LoginAction;
import org.emulinker.kaillera.controller.v086.action.PlayerDesynchAction;
import org.emulinker.kaillera.controller.v086.action.QuitAction;
import org.emulinker.kaillera.controller.v086.action.QuitGameAction;
import org.emulinker.kaillera.controller.v086.action.StartGameAction;
import org.emulinker.kaillera.controller.v086.action.UserReadyAction;
import org.emulinker.kaillera.controller.v086.action.V086Action;
import org.emulinker.kaillera.controller.v086.action.V086GameEventHandler;
import org.emulinker.kaillera.controller.v086.action.V086ServerEventHandler;
import org.emulinker.kaillera.controller.v086.action.V086UserEventHandler;
import org.emulinker.kaillera.controller.v086.protocol.AllReady;
import org.emulinker.kaillera.controller.v086.protocol.CachedGameData;
import org.emulinker.kaillera.controller.v086.protocol.Chat;
import org.emulinker.kaillera.controller.v086.protocol.ClientACK;
import org.emulinker.kaillera.controller.v086.protocol.CreateGame;
import org.emulinker.kaillera.controller.v086.protocol.GameChat;
import org.emulinker.kaillera.controller.v086.protocol.GameData;
import org.emulinker.kaillera.controller.v086.protocol.GameKick;
import org.emulinker.kaillera.controller.v086.protocol.JoinGame;
import org.emulinker.kaillera.controller.v086.protocol.KeepAlive;
import org.emulinker.kaillera.controller.v086.protocol.PlayerDrop;
import org.emulinker.kaillera.controller.v086.protocol.Quit;
import org.emulinker.kaillera.controller.v086.protocol.QuitGame;
import org.emulinker.kaillera.controller.v086.protocol.StartGame;
import org.emulinker.kaillera.controller.v086.protocol.UserInformation;
import org.emulinker.kaillera.controller.v086.protocol.V086Bundle;
import org.emulinker.kaillera.controller.v086.protocol.V086BundleFormatException;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.AllReadyEvent;
import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.event.KailleraEvent;
import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.event.ServerEvent;
import org.emulinker.kaillera.model.event.UserEvent;
import org.emulinker.kaillera.model.event.ChatEvent;
import org.emulinker.kaillera.model.event.ConnectedEvent;
import org.emulinker.kaillera.model.event.GameChatEvent;
import org.emulinker.kaillera.model.event.GameClosedEvent;
import org.emulinker.kaillera.model.event.GameCreatedEvent;
import org.emulinker.kaillera.model.event.GameDataEvent;
import org.emulinker.kaillera.model.event.GameDesynchEvent;
import org.emulinker.kaillera.model.event.GameInfoEvent;
import org.emulinker.kaillera.model.event.GameStartedEvent;
import org.emulinker.kaillera.model.event.GameStatusChangedEvent;
import org.emulinker.kaillera.model.event.GameTimeoutEvent;
import org.emulinker.kaillera.model.event.InfoMessageEvent;
import org.emulinker.kaillera.model.event.PlayerDesynchEvent;
import org.emulinker.kaillera.model.event.UserDroppedGameEvent;
import org.emulinker.kaillera.model.event.UserJoinedEvent;
import org.emulinker.kaillera.model.event.UserJoinedGameEvent;
import org.emulinker.kaillera.model.event.UserQuitEvent;
import org.emulinker.kaillera.model.event.UserQuitGameEvent;
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

public class V086Controller implements KailleraServerController {
    private static final Logger log = LoggerFactory.getLogger(V086Controller.class);

    private static final int MAX_BUNDLE_SIZE = 5;

    private final int bufferSize;
    private volatile boolean isRunning = false;

    private final EmuLinkerExecutor threadPool;
    private final KailleraServer server;
    private final AccessManager accessManager;
    private final String[] clientTypes;
    private final Map<Integer, V086ClientHandler> clientHandlers = new ConcurrentHashMap<Integer, V086ClientHandler>();

    private final int portRangeStart;
    private final int extraPorts;
    private final Queue<Integer> portRangeQueue = new ConcurrentLinkedQueue<Integer>();

    // shouldn't need to use a synchronized or concurrent map since all thread
    // access will be read only
    private final Map<Class, V086ServerEventHandler> serverEventHandlers = new HashMap<Class, V086ServerEventHandler>();
    private final Map<Class, V086GameEventHandler> gameEventHandlers = new HashMap<Class, V086GameEventHandler>();
    private final Map<Class, V086UserEventHandler> userEventHandlers = new HashMap<Class, V086UserEventHandler>();

    private final V086Action[] actions = new V086Action[25];

    public V086Controller(KailleraServer server, EmuLinkerExecutor threadPool,
            AccessManager accessManager, ControllersConfig controllersConfig,
            ServerConfig serverConfig) {
        this.threadPool = threadPool;
        this.server = server;
        this.accessManager = accessManager;

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

        // array access should be faster than a hash and we won't have to create
        // a new Integer each time
        actions[UserInformation.ID] = LoginAction.getInstance();
        actions[ClientACK.ID] = ACKAction.getInstance();
        actions[Chat.ID] = ChatAction.getInstance();
        actions[CreateGame.ID] = CreateGameAction.getInstance();
        actions[JoinGame.ID] = JoinGameAction.getInstance();
        actions[KeepAlive.ID] = KeepAliveAction.getInstance();
        actions[QuitGame.ID] = QuitGameAction.getInstance();
        actions[Quit.ID] = QuitAction.getInstance();
        actions[StartGame.ID] = StartGameAction.getInstance();
        actions[GameChat.ID] = GameChatAction.getInstance();
        actions[GameKick.ID] = GameKickAction.getInstance();
        actions[AllReady.ID] = UserReadyAction.getInstance();
        actions[CachedGameData.ID] = CachedGameDataAction.getInstance();
        actions[GameData.ID] = GameDataAction.getInstance();
        actions[PlayerDrop.ID] = DropGameAction.getInstance();

        // setup the server event handlers
        serverEventHandlers.put(ChatEvent.class, ChatAction.getInstance());
        serverEventHandlers.put(GameCreatedEvent.class, CreateGameAction.getInstance());
        serverEventHandlers.put(UserJoinedEvent.class, LoginAction.getInstance());
        serverEventHandlers.put(GameClosedEvent.class, CloseGameAction.getInstance());
        serverEventHandlers.put(UserQuitEvent.class, QuitAction.getInstance());
        serverEventHandlers.put(GameStatusChangedEvent.class, GameStatusAction.getInstance());

        // setup the game event handlers
        gameEventHandlers.put(UserJoinedGameEvent.class, JoinGameAction.getInstance());
        gameEventHandlers.put(UserQuitGameEvent.class, QuitGameAction.getInstance());
        gameEventHandlers.put(GameStartedEvent.class, StartGameAction.getInstance());
        gameEventHandlers.put(GameChatEvent.class, GameChatAction.getInstance());
        gameEventHandlers.put(AllReadyEvent.class, UserReadyAction.getInstance());
        gameEventHandlers.put(GameDataEvent.class, GameDataAction.getInstance());
        gameEventHandlers.put(UserDroppedGameEvent.class, DropGameAction.getInstance());
        gameEventHandlers.put(GameDesynchEvent.class, GameDesynchAction.getInstance());
        gameEventHandlers.put(PlayerDesynchEvent.class, PlayerDesynchAction.getInstance());
        gameEventHandlers.put(GameInfoEvent.class, GameInfoAction.getInstance());
        gameEventHandlers.put(GameTimeoutEvent.class, GameTimeoutAction.getInstance());
        gameEventHandlers.put(GameTimeoutEvent.class, GameTimeoutAction.getInstance());

        // setup the user event handlers
        userEventHandlers.put(ConnectedEvent.class, ACKAction.getInstance());
        userEventHandlers.put(InfoMessageEvent.class, InfoMessageAction.getInstance());
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

    public final Map<Class, V086ServerEventHandler> getServerEventHandlers() {
        return serverEventHandlers;
    }

    public final Map<Class, V086GameEventHandler> getGameEventHandlers() {
        return gameEventHandlers;
    }

    public final Map<Class, V086UserEventHandler> getUserEventHandlers() {
        return userEventHandlers;
    }

    public final V086Action[] getActions() {
        return actions;
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

    public class V086ClientHandler extends PrivateUDPServer implements KailleraEventListener {
        private KailleraUser user;
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
            Thread.yield();

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
                log.debug(this.toString() + " Stoping!");
                super.stop();

                if (port > 0) {
                    log.debug(toString() + " returning port " + port + " to available port queue: "
                            + (portRangeQueue.size() + 1) + " available");
                    portRangeQueue.add(port);
                }
            }

            if (user != null) {
                clientHandlers.remove(user.getID());
                user.stop();
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

                            V086Action action = actions[messages[i].getID()];
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
                Thread.yield();
                stop();
            }
        }

        public void actionPerformed(KailleraEvent event) {
            if (event instanceof GameEvent) {
                V086GameEventHandler eventHandler = gameEventHandlers.get(event.getClass());
                if (eventHandler == null) {
                    log.error(toString()
                            + " found no GameEventHandler registered to handle game event: "
                            + event);
                    return;
                }

                eventHandler.handleEvent((GameEvent) event, this);
            } else if (event instanceof ServerEvent) {
                V086ServerEventHandler eventHandler = serverEventHandlers.get(event.getClass());
                if (eventHandler == null) {
                    log.error(toString()
                            + " found no ServerEventHandler registered to handle server event: "
                            + event);
                    return;
                }

                eventHandler.handleEvent((ServerEvent) event, this);
            } else if (event instanceof UserEvent) {
                V086UserEventHandler eventHandler = userEventHandlers.get(event.getClass());
                if (eventHandler == null) {
                    log.error(toString()
                            + " found no UserEventHandler registered to handle user event: "
                            + event);
                    return;
                }

                eventHandler.handleEvent((UserEvent) event, this);
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
