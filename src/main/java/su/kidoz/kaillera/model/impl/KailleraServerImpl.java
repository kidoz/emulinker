package su.kidoz.kaillera.model.impl;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import su.kidoz.config.GameConfig;
import su.kidoz.config.MasterListConfig;
import su.kidoz.config.ServerConfig;
import su.kidoz.kaillera.access.AccessManager;
import su.kidoz.kaillera.master.StatsCollector;
import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraServer;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.ChatEvent;
import su.kidoz.kaillera.model.event.ConnectedEvent;
import su.kidoz.kaillera.model.event.GameClosedEvent;
import su.kidoz.kaillera.model.event.GameCreatedEvent;
import su.kidoz.kaillera.model.event.EventDispatcher;
import su.kidoz.kaillera.model.event.ServerEvent;
import su.kidoz.kaillera.model.event.UserQuitEvent;
import su.kidoz.kaillera.model.exception.ChatException;
import su.kidoz.kaillera.model.exception.ClientAddressException;
import su.kidoz.kaillera.model.exception.CloseGameException;
import su.kidoz.kaillera.model.exception.ConnectionTypeException;
import su.kidoz.kaillera.model.exception.CreateGameException;
import su.kidoz.kaillera.model.exception.DropGameException;
import su.kidoz.kaillera.model.exception.FloodException;
import su.kidoz.kaillera.model.exception.LoginException;
import su.kidoz.kaillera.model.exception.NewConnectionException;
import su.kidoz.kaillera.model.exception.PingTimeException;
import su.kidoz.kaillera.model.exception.QuitException;
import su.kidoz.kaillera.model.exception.QuitGameException;
import su.kidoz.kaillera.model.exception.ServerFullException;
import su.kidoz.kaillera.model.exception.UserNameException;
import su.kidoz.release.ReleaseInfo;
import su.kidoz.util.EmuLang;
import su.kidoz.util.EmuLinkerExecutor;
import su.kidoz.util.EmuUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import su.kidoz.kaillera.model.event.LoginProgressEvent;

import su.kidoz.kaillera.model.LoginNotificationState;
import su.kidoz.kaillera.model.validation.LoginValidator;
import su.kidoz.kaillera.service.AnnouncementService;
import su.kidoz.kaillera.service.ChatModerationService;

/**
 * Main implementation of the Kaillera server. Manages users, games, and
 * coordinates all server operations for multiplayer emulator gaming over UDP.
 *
 * <p>
 * This class uses fine-grained locking for concurrent operations:
 * <ul>
 * <li>{@code serverLifecycleLock} - for start/stop operations</li>
 * <li>{@code connectionLock} - for user connection/login/quit operations</li>
 * <li>{@code gameLock} - for game creation/closure operations</li>
 * </ul>
 *
 * @see KailleraServer
 * @see KailleraUser
 * @see KailleraGame
 */
public class KailleraServerImpl implements KailleraServer, Runnable, SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(KailleraServerImpl.class);

    // Configuration objects - access values via getters instead of copying
    private final ServerConfig serverConfig;
    private final GameConfig gameConfig;

    private final List<String> loginMessages = new ArrayList<>();

    private volatile boolean stopFlag = false;
    private volatile boolean isRunning = false;

    private final EmuLinkerExecutor threadPool;
    private final AccessManager accessManager;
    private StatsCollector statsCollector;
    private final ReleaseInfo releaseInfo;
    private final AutoFireDetectorFactory autoFireDetectorFactory;
    private final LoginValidator loginValidator;
    private final ChatModerationService chatModerationService;
    private final AnnouncementService announcementService;
    private final UserManager userManager;
    private final GameManager gameManager;
    private ServerMaintenanceTask maintenanceTask;

    // Fine-grained locks replacing coarse synchronized(this)
    private final Lock serverLifecycleLock = new ReentrantLock();
    private final Lock connectionLock = new ReentrantLock();
    private final Lock gameLock = new ReentrantLock();

    /**
     * Creates a new Kaillera server instance.
     *
     * @param threadPool
     *            executor for virtual threads
     * @param accessManager
     *            handles user access control and banning
     * @param serverConfig
     *            server configuration (max users, timeouts, etc.)
     * @param gameConfig
     *            game configuration (buffer size, timeouts)
     * @param masterListConfig
     *            master server list configuration
     * @param statsCollector
     *            collects statistics for master list reporting
     * @param releaseInfo
     *            version and release information
     * @param autoFireDetectorFactory
     *            factory for creating auto-fire detectors
     * @param loginValidator
     *            validates user login requests
     * @param chatModerationService
     *            handles chat validation and flood control
     * @param announcementService
     *            sends announcements to users
     * @param userManager
     *            manages user storage and lifecycle
     * @param gameManager
     *            manages game storage and lifecycle
     */
    public KailleraServerImpl(EmuLinkerExecutor threadPool, AccessManager accessManager,
            ServerConfig serverConfig, GameConfig gameConfig, MasterListConfig masterListConfig,
            StatsCollector statsCollector, ReleaseInfo releaseInfo,
            AutoFireDetectorFactory autoFireDetectorFactory, LoginValidator loginValidator,
            ChatModerationService chatModerationService, AnnouncementService announcementService,
            UserManager userManager, GameManager gameManager) {
        this.threadPool = threadPool;
        this.accessManager = accessManager;
        this.releaseInfo = releaseInfo;
        this.autoFireDetectorFactory = autoFireDetectorFactory;
        this.serverConfig = serverConfig;
        this.gameConfig = gameConfig;
        this.loginValidator = loginValidator;
        this.chatModerationService = chatModerationService;
        this.announcementService = announcementService;
        this.userManager = userManager;
        this.gameManager = gameManager;

        // Load login messages from language bundle
        for (int i = 1; i <= 999; i++) {
            if (EmuLang.hasString("KailleraServerImpl.LoginMessage." + i))
                loginMessages.add(EmuLang.getString("KailleraServerImpl.LoginMessage." + i));
            else
                break;
        }

        // Master list config
        if (masterListConfig.isTouchKaillera())
            this.statsCollector = statsCollector;
    }

    @Override
    public AccessManager getAccessManager() {
        return accessManager;
    }

    public KailleraUser getUser(int userID) {
        return userManager.getUser(userID);
    }

    public KailleraGame getGame(int gameID) {
        return gameManager.getGame(gameID);
    }

    public Collection<KailleraUserImpl> getUsers() {
        return userManager.getUsers();
    }

    public Collection<KailleraGameImpl> getGames() {
        return gameManager.getGames();
    }

    public int getNumUsers() {
        return userManager.getNumUsers();
    }

    public int getNumGames() {
        return gameManager.getNumGames();
    }

    public int getNumGamesPlaying() {
        return gameManager.getNumGamesPlaying();
    }

    public int getMaxPing() {
        return serverConfig.getMaxPing();
    }

    public int getMaxUsers() {
        return serverConfig.getMaxUsers();
    }

    public int getMaxGames() {
        return serverConfig.getMaxGames();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public EmuLinkerExecutor getThreadPool() {
        return threadPool;
    }

    boolean getAllowSinglePlayer() {
        return serverConfig.isAllowSinglePlayer();
    }

    public String toString() {
        return "KailleraServerImpl[numUsers=" + getNumUsers() + " numGames=" + getNumGames()
                + " isRunning=" + isRunning() + "]";
    }

    @Override
    public void start() {
        serverLifecycleLock.lock();
        try {
            if (isRunning) {
                log.debug("KailleraServer start request ignored: already running!");
                return;
            }

            log.debug("KailleraServer thread starting (ThreadPool:" + threadPool.getActiveCount()
                    + "/" + threadPool.getPoolSize() + ")");
            stopFlag = false;
            threadPool.execute(this);
            log.info("KailleraServer started");
        } finally {
            serverLifecycleLock.unlock();
        }
    }

    @Override
    public void stop() {
        serverLifecycleLock.lock();
        try {
            log.debug("KailleraServer thread received stop request!");

            if (!isRunning()) {
                log.debug("KailleraServer thread stop request ignored: not running!");
                return;
            }

            stopFlag = true;
            if (maintenanceTask != null) {
                maintenanceTask.stop();
            }

            userManager.stopAllUsers();
            gameManager.clear();
            log.info("KailleraServer stopped");
        } finally {
            serverLifecycleLock.unlock();
        }
    }

    @Override
    public int getPhase() {
        // Phase 10: Core server logic, after access manager
        return 10;
    }

    StatsCollector getStatsCollector() {
        return statsCollector;
    }

    AutoFireDetector getAutoFireDetector(KailleraGame game) {
        int sensitivity = gameConfig.getDefaultAutoFireSensitivity();
        if (sensitivity == 0)
            return null;
        return autoFireDetectorFactory.getInstance(game, sensitivity);
    }

    public ReleaseInfo getReleaseInfo() {
        return releaseInfo;
    }

    public KailleraUser newConnection(InetSocketAddress clientSocketAddress, String protocol,
            EventDispatcher eventDispatcher) throws ServerFullException, NewConnectionException {
        // we'll assume at this point that ConnectController has already asked
        // AccessManager if this IP is banned, so no need to do it again here

        log.debug("Processing connection request from "
                + EmuUtil.formatSocketAddress(clientSocketAddress));

        int access = accessManager.getAccess(clientSocketAddress.getAddress());

        connectionLock.lock();
        try {
            // admins will be allowed in even if the server is full
            if (getMaxUsers() > 0 && userManager.getNumUsers() >= getMaxUsers()
                    && !(access > AccessManager.ACCESS_NORMAL)) {
                log.warn("Connection from " + EmuUtil.formatSocketAddress(clientSocketAddress)
                        + " denied: Server is full!");
                throw new ServerFullException(
                        EmuLang.getString("KailleraServerImpl.LoginDeniedServerFull"));
            }

            int userID;
            try {
                userID = userManager.getNextUserID();
            } catch (IllegalStateException e) {
                log.warn("Connection denied: no available user IDs");
                throw new ServerFullException(
                        EmuLang.getString("KailleraServerImpl.LoginDeniedServerFull"));
            }
            KailleraUserImpl user = new KailleraUserImpl(userID, protocol, clientSocketAddress,
                    eventDispatcher, this);
            user.setStatus(KailleraUser.STATUS_CONNECTING);

            log.info(user + " attempting new connection using protocol " + protocol + " from "
                    + EmuUtil.formatSocketAddress(clientSocketAddress));

            log.debug(user + " Thread starting (ThreadPool:" + threadPool.getActiveCount() + "/"
                    + threadPool.getPoolSize() + ")");
            threadPool.execute(user);
            log.debug(user + " Thread started (ThreadPool:" + threadPool.getActiveCount() + "/"
                    + threadPool.getPoolSize() + ")");
            userManager.addUser(user);

            return user;
        } finally {
            connectionLock.unlock();
        }
    }

    public void login(KailleraUser user) throws PingTimeException, ClientAddressException,
            ConnectionTypeException, UserNameException, LoginException {
        KailleraUserImpl userImpl = (KailleraUserImpl) user;

        long loginDelay = (System.currentTimeMillis() - user.getConnectTime());
        log.info(user + ": login request: delay=" + loginDelay + "ms, clientAddress="
                + EmuUtil.formatSocketAddress(user.getSocketAddress()) + ", name=" + user.getName()
                + ", ping=" + user.getPing() + ", client=" + user.getClientType() + ", connection="
                + KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]);

        int userID = user.getID();

        connectionLock.lock();
        try {
            KailleraUser userFromList = userManager.getUser(userID);

            // Run all validations - exceptions are thrown on failure
            try {
                loginValidator.validateNotAlreadyLoggedIn(user);
                loginValidator.validateUserExists(user, userFromList);
                int access = loginValidator.validateAccessLevel(user);
                loginValidator.validatePing(user, access);
                loginValidator.validateConnectionType(user, access);
                loginValidator.validateUserName(user, access);
                loginValidator.validateClientName(user, access);
                loginValidator.validateUserStatus(user, userFromList);
                loginValidator.validateAddressMatch(user, userFromList);
                loginValidator.validateEmulator(user, access);

                // Check for duplicate login - may return a user to force quit
                KailleraUserImpl reconnectUser = loginValidator.checkDuplicateLogin(user,
                        userFromList, getUsers(), access);
                if (reconnectUser != null) {
                    try {
                        quitInternal(reconnectUser,
                                EmuLang.getString("KailleraServerImpl.ForcedQuitReconnected"));
                    } catch (Exception e) {
                        log.error("Error forcing " + reconnectUser + " quit for reconnect!", e);
                    }
                }

                // Passed all checks - complete login
                userImpl.setAccess(access);
                userImpl.setStatus(KailleraUser.STATUS_IDLE);
                userImpl.setLoggedIn();
                userManager.addUser(userImpl);
                userImpl.addEvent(new ConnectedEvent(this, user));

                // Release lock before sleeping to allow other logins to proceed
                sendLoginNotificationsAsync(userImpl, access);
            } catch (LoginException e) {
                // Remove user from list on validation failure (LoginException is parent of all)
                userManager.removeUser(userID);
                throw e;
            }
        } finally {
            connectionLock.unlock();
        }
    }

    /**
     * Starts the login notification sequence using event-driven state machine.
     * Replaces the previous Thread.sleep() approach with proper event chaining via
     * LoginProgressEvent.
     *
     * <p>
     * The notification sequence is:
     *
     * <pre>
     * CONNECTED → USER_JOINED → MESSAGES_SENT → ADMIN_INFO → COMPLETE
     * </pre>
     *
     * @param userImpl
     *            the user who just logged in
     * @param access
     *            the user's access level
     */
    private void sendLoginNotificationsAsync(KailleraUserImpl userImpl, int access) {
        // Queue the first login progress event - the state machine will handle the rest
        LoginProgressEvent progressEvent = new LoginProgressEvent(this, userImpl,
                LoginNotificationState.CONNECTED, List.copyOf(loginMessages), access);
        userImpl.addEvent(progressEvent);
    }

    public void quit(KailleraUser user, String message)
            throws QuitException, DropGameException, QuitGameException, CloseGameException {
        connectionLock.lock();
        try {
            quitInternal(user, message);
        } finally {
            connectionLock.unlock();
        }
    }

    /**
     * Internal quit implementation - assumes connectionLock is already held. Used
     * to avoid deadlock when called from login() which already holds the lock.
     */
    private void quitInternal(KailleraUser user, String message)
            throws QuitException, DropGameException, QuitGameException, CloseGameException {
        if (!user.isLoggedIn()) {
            userManager.removeUser(user.getID());
            log.error(user + " quit failed: Not logged in");
            throw new QuitException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
        }

        if (userManager.removeUser(user.getID()) == null)
            log.error(user + " quit failed: not in user list");

        if (user.getGame() != null)
            user.quitGame();

        String quitMsg = message.trim();
        int maxLen = serverConfig.getMaxQuitMessageLength();
        if (quitMsg.isEmpty() || (maxLen > 0 && quitMsg.length() > maxLen))
            quitMsg = EmuLang.getString("KailleraServerImpl.StandardQuitMessage");

        log.info(user + " quit: " + quitMsg);

        UserQuitEvent quitEvent = new UserQuitEvent(this, user, quitMsg);

        addEvent(quitEvent);
        ((KailleraUserImpl) user).addEvent(quitEvent);
    }

    public void chat(KailleraUser user, String message) throws ChatException, FloodException {
        // No lock needed - chatModerationService handles flood control thread-safely,
        // and addEvent iterates over a thread-safe collection
        String validatedMessage = chatModerationService.validateChat(user, message);
        if (validatedMessage.isEmpty()) {
            return;
        }

        log.info(user + " chat: " + validatedMessage);
        addEvent(new ChatEvent(this, user, validatedMessage));
    }

    public KailleraGame createGame(KailleraUser user, String romName)
            throws CreateGameException, FloodException {
        if (!user.isLoggedIn()) {
            log.error(user + " create game failed: Not logged in");
            throw new CreateGameException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
        }

        if (((KailleraUserImpl) user).getGame() != null) {
            log.error(user + " create game failed: already in game: "
                    + ((KailleraUserImpl) user).getGame());
            throw new CreateGameException(
                    EmuLang.getString("KailleraServerImpl.CreateGameErrorAlreadyInGame"));
        }

        int access = accessManager.getAccess(user.getSocketAddress().getAddress());
        int floodTime = serverConfig.getCreateGameFloodTime();
        int maxNameLen = serverConfig.getMaxGameNameLength();

        if (access == AccessManager.ACCESS_NORMAL) {
            if (floodTime > 0 && (System.currentTimeMillis()
                    - ((KailleraUserImpl) user).getLastCreateGameTime()) < (floodTime * 1000)) {
                log.warn(user + " create game denied: Flood: " + romName);
                throw new FloodException(
                        EmuLang.getString("KailleraServerImpl.CreateGameDeniedFloodControl"));
            }

            if (containsIllegalCharacters(romName)) {
                log.warn(user + " create game denied: Illegal characters in ROM name");
                throw new CreateGameException(
                        EmuLang.getString("KailleraServerImpl.CreateGameDeniedIllegalCharacters"));
            }

            if (romName.trim().isEmpty()) {
                log.warn(user + " create game denied: Rom Name Empty");
                throw new CreateGameException(
                        EmuLang.getString("KailleraServerImpl.CreateGameErrorEmptyName"));
            }

            if (maxNameLen > 0 && romName.trim().length() > maxNameLen) {
                log.warn(user + " create game denied: Rom Name Length > " + maxNameLen);
                throw new CreateGameException(
                        EmuLang.getString("KailleraServerImpl.CreateGameDeniedNameTooLong"));
            }

            if (!accessManager.isGameAllowed(romName)) {
                log.warn(user + " create game denied: AccessManager denied game: " + romName);
                throw new CreateGameException(
                        EmuLang.getString("KailleraServerImpl.CreateGameDeniedGameBanned"));
            }
        }

        gameLock.lock();
        try {
            // Re-check max games inside lock to ensure atomicity
            int maxGames = serverConfig.getMaxGames();
            if (access == AccessManager.ACCESS_NORMAL && maxGames > 0
                    && getNumGames() >= maxGames) {
                log.warn(user + " create game denied: Over maximum of " + maxGames
                        + " current games!");
                throw new CreateGameException(
                        EmuLang.getString("KailleraServerImpl.CreateGameDeniedMaxGames", maxGames));
            }

            int gameID;
            try {
                gameID = gameManager.getNextGameID();
            } catch (IllegalStateException e) {
                log.warn(user + " create game denied: no available game IDs");
                throw new CreateGameException(EmuLang.getString(
                        "KailleraServerImpl.CreateGameDeniedMaxGames", serverConfig.getMaxGames()));
            }
            KailleraGameImpl game = new KailleraGameImpl(gameID, romName, (KailleraUserImpl) user,
                    this, gameConfig.getBufferSize(), gameConfig.getTimeoutMillis(),
                    gameConfig.getDesynchTimeouts());
            gameManager.addGame(game);

            addEvent(new GameCreatedEvent(this, game));

            log.info(user + " created: " + game + ": " + game.getRomName());

            try {
                user.joinGame(game.getID());
            } catch (Exception e) {
                // this shouldn't happen
                log.error("Caught exception while making owner join game! This shouldn't happen!",
                        e);
            }

            announce(EmuLang.getString("KailleraServerImpl.UserCreatedGameAnnouncement",
                    user.getName(), game.getRomName()), false);

            return game;
        } finally {
            gameLock.unlock();
        }
    }

    void closeGame(KailleraGame game, KailleraUser user) throws CloseGameException {
        if (!user.isLoggedIn()) {
            log.error(user + " close " + game + " failed: Not logged in");
            throw new CloseGameException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
        }

        gameLock.lock();
        try {
            if (!gameManager.containsGame(game.getID())) {
                log.error(user + " close " + game + " failed: not in list: " + game);
                return;
            }

            ((KailleraGameImpl) game).close(user);
            gameManager.removeGame(game.getID());

            log.info(user + " closed: " + game);
            addEvent(new GameClosedEvent(this, game));
        } finally {
            gameLock.unlock();
        }
    }

    @Override
    public void announce(String announcement, boolean gamesAlso) {
        announcementService.announce(announcement, gamesAlso, getUsers(), getGames());
    }

    @Override
    public void addEvent(ServerEvent event) {
        for (KailleraUserImpl user : userManager.getUsers()) {
            if (user.isLoggedIn())
                user.addEvent(event);
            else {
                log.debug(user + ": not adding event, not logged in: " + event);
            }
        }
    }

    public void run() {
        isRunning = true;
        log.debug("KailleraServer thread running...");

        try {
            // Create and run maintenance task - it will handle timeouts, bans, etc.
            maintenanceTask = new ServerMaintenanceTask(userManager, accessManager,
                    serverConfig.getMaxPing(), serverConfig.getKeepAliveTimeout(),
                    serverConfig.getIdleTimeout(), this::handleQuitRequest);
            maintenanceTask.run();
        } catch (Throwable e) {
            if (!stopFlag)
                log.error("KailleraServer thread caught unexpected exception: " + e, e);
        } finally {
            isRunning = false;
            log.debug("KailleraServer thread exiting...");
        }
    }

    /**
     * Handles quit requests from the maintenance task.
     */
    private void handleQuitRequest(ServerMaintenanceTask.UserQuitRequest request) {
        try {
            quit(request.user(), request.message());
        } catch (Exception e) {
            log.error("Error forcing " + request.user() + " quit: " + request.message(), e);
        }
    }

    /**
     * Checks if a string contains illegal characters (control chars, dangerous
     * Unicode).
     *
     * @param str
     *            the string to check
     * @return true if null, empty, or illegal characters are found
     */
    private boolean containsIllegalCharacters(String str) {
        if (str == null || str.isEmpty()) {
            return true; // Null/empty strings are invalid
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            // Check for control characters (< 32), DEL (127), and other ISO control chars
            if (c < 32 || c == 127 || Character.isISOControl(c)) {
                return true;
            }
            // Check for Unicode direction override characters (potential spoofing)
            if (c == '\u202A' || c == '\u202B' || c == '\u202C' || c == '\u202D' || c == '\u202E'
                    || c == '\u2066' || c == '\u2067' || c == '\u2068' || c == '\u2069') {
                return true;
            }
            // Check for zero-width characters (potential spoofing)
            if (c == '\u200B' || c == '\u200C' || c == '\u200D' || c == '\uFEFF') {
                return true;
            }
        }
        return false;
    }
}
