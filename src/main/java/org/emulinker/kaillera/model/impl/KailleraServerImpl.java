package org.emulinker.kaillera.model.impl;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.emulinker.config.GameConfig;
import org.emulinker.config.MasterListConfig;
import org.emulinker.config.ServerConfig;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.ChatEvent;
import org.emulinker.kaillera.model.event.ConnectedEvent;
import org.emulinker.kaillera.model.event.GameClosedEvent;
import org.emulinker.kaillera.model.event.GameCreatedEvent;
import org.emulinker.kaillera.model.event.InfoMessageEvent;
import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.event.ServerEvent;
import org.emulinker.kaillera.model.event.UserJoinedEvent;
import org.emulinker.kaillera.model.event.UserQuitEvent;
import org.emulinker.kaillera.model.exception.ChatException;
import org.emulinker.kaillera.model.exception.ClientAddressException;
import org.emulinker.kaillera.model.exception.CloseGameException;
import org.emulinker.kaillera.model.exception.ConnectionTypeException;
import org.emulinker.kaillera.model.exception.CreateGameException;
import org.emulinker.kaillera.model.exception.DropGameException;
import org.emulinker.kaillera.model.exception.FloodException;
import org.emulinker.kaillera.model.exception.LoginException;
import org.emulinker.kaillera.model.exception.NewConnectionException;
import org.emulinker.kaillera.model.exception.PingTimeException;
import org.emulinker.kaillera.model.exception.QuitException;
import org.emulinker.kaillera.model.exception.QuitGameException;
import org.emulinker.kaillera.model.exception.ServerFullException;
import org.emulinker.kaillera.model.exception.UserNameException;
import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.EmuLang;
import org.emulinker.util.EmuLinkerExecutor;
import org.emulinker.util.EmuUtil;
import org.emulinker.util.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.kidoz.kaillera.model.impl.GameManager;
import su.kidoz.kaillera.model.impl.ServerMaintenanceTask;
import su.kidoz.kaillera.model.impl.UserManager;
import su.kidoz.kaillera.model.validation.LoginValidator;
import su.kidoz.kaillera.service.AnnouncementService;
import su.kidoz.kaillera.service.ChatModerationService;

public class KailleraServerImpl implements KailleraServer, Executable {
    private static final Logger log = LoggerFactory.getLogger(KailleraServerImpl.class);

    private final int maxPing;
    private final int maxUsers;
    private final int maxGames;
    private final int idleTimeout;
    private final int keepAliveTimeout;
    private final int chatFloodTime;
    private final int createGameFloodTime;
    private final int maxUserNameLength;
    private final int maxChatLength;
    private final int maxGameNameLength;
    private final int maxQuitMessageLength;
    private final int maxClientNameLength;

    private final int gameBufferSize;
    private final int gameTimeoutMillis;
    private final int gameDesynchTimeouts;
    private final int gameAutoFireSensitivity;

    private final ServerConfig serverConfig;

    private final List<String> loginMessages = new ArrayList<String>();
    private final boolean allowSinglePlayer;
    private final boolean allowMultipleConnections;

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

    public KailleraServerImpl(EmuLinkerExecutor threadPool, AccessManager accessManager,
            ServerConfig serverConfig, GameConfig gameConfig, MasterListConfig masterListConfig,
            StatsCollector statsCollector, ReleaseInfo releaseInfo,
            AutoFireDetectorFactory autoFireDetectorFactory) {
        this.threadPool = threadPool;
        this.accessManager = accessManager;
        this.releaseInfo = releaseInfo;
        this.autoFireDetectorFactory = autoFireDetectorFactory;
        this.serverConfig = serverConfig;
        this.loginValidator = new LoginValidator(accessManager, serverConfig);
        this.announcementService = new AnnouncementService();

        // Server config
        this.maxPing = serverConfig.getMaxPing();
        this.maxUsers = serverConfig.getMaxUsers();
        this.maxGames = serverConfig.getMaxGames();
        this.keepAliveTimeout = serverConfig.getKeepAliveTimeout();
        this.idleTimeout = serverConfig.getIdleTimeout();
        this.chatFloodTime = serverConfig.getChatFloodTime();
        this.createGameFloodTime = serverConfig.getCreateGameFloodTime();
        this.allowSinglePlayer = serverConfig.isAllowSinglePlayer();
        this.allowMultipleConnections = serverConfig.isAllowMultipleConnections();
        this.maxUserNameLength = serverConfig.getMaxUserNameLength();
        this.maxChatLength = serverConfig.getMaxChatLength();
        this.maxGameNameLength = serverConfig.getMaxGameNameLength();
        this.maxQuitMessageLength = serverConfig.getMaxQuitMessageLength();
        this.maxClientNameLength = serverConfig.getMaxClientNameLength();

        // Initialize services that depend on config values
        this.chatModerationService = new ChatModerationService(accessManager, chatFloodTime,
                maxChatLength);

        // Load login messages from language bundle
        for (int i = 1; i <= 999; i++) {
            if (EmuLang.hasString("KailleraServerImpl.LoginMessage." + i))
                loginMessages.add(EmuLang.getString("KailleraServerImpl.LoginMessage." + i));
            else
                break;
        }

        // Game config
        this.gameBufferSize = gameConfig.getBufferSize();
        this.gameTimeoutMillis = gameConfig.getTimeoutMillis();
        this.gameDesynchTimeouts = gameConfig.getDesynchTimeouts();
        this.gameAutoFireSensitivity = gameConfig.getDefaultAutoFireSensitivity();

        // Initialize managers
        this.userManager = new UserManager(maxUsers);
        this.gameManager = new GameManager(maxUsers);

        // Master list config
        if (masterListConfig.isTouchKaillera())
            this.statsCollector = statsCollector;
    }

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
        return maxPing;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public int getMaxGames() {
        return maxGames;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public EmuLinkerExecutor getThreadPool() {
        return threadPool;
    }

    boolean getAllowSinglePlayer() {
        return allowSinglePlayer;
    }

    public String toString() {
        return "KailleraServerImpl[numUsers=" + getNumUsers() + " numGames=" + getNumGames()
                + " isRunning=" + isRunning() + "]";
    }

    public synchronized void start() {
        log.debug("KailleraServer thread received start request!");
        log.debug("KailleraServer thread starting (ThreadPool:" + threadPool.getActiveCount() + "/"
                + threadPool.getPoolSize() + ")");
        stopFlag = false;
        threadPool.execute(this);
    }

    public synchronized void stop() {
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
    }

    StatsCollector getStatsCollector() {
        return statsCollector;
    }

    AutoFireDetector getAutoFireDetector(KailleraGame game) {
        if (gameAutoFireSensitivity == 0)
            return null;
        return autoFireDetectorFactory.getInstance(game, gameAutoFireSensitivity);
    }

    public ReleaseInfo getReleaseInfo() {
        return releaseInfo;
    }

    public synchronized KailleraUser newConnection(InetSocketAddress clientSocketAddress,
            String protocol, KailleraEventListener listener)
            throws ServerFullException, NewConnectionException {
        // we'll assume at this point that ConnectController has already asked
        // AccessManager if this IP is banned, so no need to do it again here

        log.debug("Processing connection request from "
                + EmuUtil.formatSocketAddress(clientSocketAddress));

        int access = accessManager.getAccess(clientSocketAddress.getAddress());

        // admins will be allowed in even if the server is full
        if (getMaxUsers() > 0 && userManager.getNumUsers() >= getMaxUsers()
                && !(access > AccessManager.ACCESS_NORMAL)) {
            log.warn("Connection from " + EmuUtil.formatSocketAddress(clientSocketAddress)
                    + " denied: Server is full!");
            throw new ServerFullException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedServerFull"));
        }

        int userID = userManager.getNextUserID();
        KailleraUserImpl user = new KailleraUserImpl(userID, protocol, clientSocketAddress,
                listener, this);
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
    }

    public synchronized void login(KailleraUser user) throws PingTimeException,
            ClientAddressException, ConnectionTypeException, UserNameException, LoginException {
        KailleraUserImpl userImpl = (KailleraUserImpl) user;

        long loginDelay = (System.currentTimeMillis() - user.getConnectTime());
        log.info(user + ": login request: delay=" + loginDelay + "ms, clientAddress="
                + EmuUtil.formatSocketAddress(user.getSocketAddress()) + ", name=" + user.getName()
                + ", ping=" + user.getPing() + ", client=" + user.getClientType() + ", connection="
                + KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]);

        int userID = user.getID();
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
            KailleraUserImpl reconnectUser = loginValidator.checkDuplicateLogin(user, userFromList,
                    getUsers(), access);
            if (reconnectUser != null) {
                try {
                    quit(reconnectUser,
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
    }

    /**
     * Sends login notifications asynchronously to avoid blocking the synchronized
     * login method. This allows other users to login concurrently while
     * notifications are being sent.
     */
    private void sendLoginNotificationsAsync(KailleraUserImpl userImpl, int access) {
        threadPool.execute(() -> {
            try {
                // Small delay to allow client to process ConnectedEvent
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            addEvent(new UserJoinedEvent(this, userImpl));

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            for (String loginMessage : loginMessages) {
                userImpl.addEvent(new InfoMessageEvent(userImpl, loginMessage));
            }

            if (access > AccessManager.ACCESS_NORMAL) {
                log.info(userImpl + " logged in successfully with "
                        + AccessManager.ACCESS_NAMES[access] + " access!");
            } else {
                log.info(userImpl + " logged in successfully");
            }

            String announcement = accessManager
                    .getAnnouncement(userImpl.getSocketAddress().getAddress());
            if (announcement != null) {
                announce(announcement, false);
            }

            if (access == AccessManager.ACCESS_ADMIN) {
                userImpl.addEvent(new InfoMessageEvent(userImpl,
                        EmuLang.getString("KailleraServerImpl.AdminWelcomeMessage")));
            }

            // Send Kaillux client-specific info
            if (userImpl.isEmuLinkerClient()) {
                userImpl.addEvent(
                        new InfoMessageEvent(userImpl, ":ACCESS=" + userImpl.getAccessStr()));

                if (access == AccessManager.ACCESS_ADMIN) {
                    sendAdminUserInfo(userImpl);
                }
            }
        });
    }

    /**
     * Sends user info to admin Kaillux clients.
     */
    private void sendAdminUserInfo(KailleraUserImpl admin) {
        StringBuilder sb = new StringBuilder();
        sb.append(":USERINFO=");
        int sbCount = 0;

        for (KailleraUserImpl u3 : getUsers()) {
            if (!u3.isLoggedIn()) {
                continue;
            }

            sb.append(u3.getID());
            sb.append(",");
            sb.append(u3.getConnectSocketAddress().getAddress().getHostAddress());
            sb.append(",");
            sb.append(u3.getAccessStr());
            sb.append(";");
            sbCount++;

            if (sb.length() > 300) {
                admin.addEvent(new InfoMessageEvent(admin, sb.toString()));
                sb = new StringBuilder();
                sb.append(":USERINFO=");
                sbCount = 0;
            }
        }

        if (sbCount > 0) {
            admin.addEvent(new InfoMessageEvent(admin, sb.toString()));
        }
    }

    public synchronized void quit(KailleraUser user, String message)
            throws QuitException, DropGameException, QuitGameException, CloseGameException {
        if (!user.isLoggedIn()) {
            userManager.removeUser(user.getID());
            log.error(user + " quit failed: Not logged in");
            throw new QuitException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
        }

        if (userManager.removeUser(user.getID()) == null)
            log.error(user + " quit failed: not in user list");

        KailleraGameImpl userGame = ((KailleraUserImpl) user).getGame();
        if (userGame != null)
            user.quitGame();

        String quitMsg = message.trim();
        if (quitMsg.isEmpty()
                || (maxQuitMessageLength > 0 && quitMsg.length() > maxQuitMessageLength))
            quitMsg = EmuLang.getString("KailleraServerImpl.StandardQuitMessage");

        log.info(user + " quit: " + quitMsg);

        UserQuitEvent quitEvent = new UserQuitEvent(this, user, quitMsg);

        addEvent(quitEvent);
        ((KailleraUserImpl) user).addEvent(quitEvent);
    }

    public synchronized void chat(KailleraUser user, String message)
            throws ChatException, FloodException {
        String validatedMessage = chatModerationService.validateChat(user, message);
        if (validatedMessage.isEmpty()) {
            return;
        }

        log.info(user + " chat: " + validatedMessage);
        addEvent(new ChatEvent(this, user, validatedMessage));
    }

    public synchronized KailleraGame createGame(KailleraUser user, String romName)
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
        if (access == AccessManager.ACCESS_NORMAL) {
            if (createGameFloodTime > 0 && (System.currentTimeMillis()
                    - ((KailleraUserImpl) user).getLastCreateGameTime()) < (createGameFloodTime
                            * 1000)) {
                log.warn(user + " create game denied: Flood: " + romName);
                throw new FloodException(
                        EmuLang.getString("KailleraServerImpl.CreateGameDeniedFloodControl"));
            }

            if (maxGames > 0 && getNumGames() >= maxGames) {
                log.warn(user + " create game denied: Over maximum of " + maxGames
                        + " current games!");
                throw new CreateGameException(
                        EmuLang.getString("KailleraServerImpl.CreateGameDeniedMaxGames", maxGames));
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

            if (maxGameNameLength > 0 && romName.trim().length() > maxGameNameLength) {
                log.warn(user + " create game denied: Rom Name Length > " + maxGameNameLength);
                throw new CreateGameException(
                        EmuLang.getString("KailleraServerImpl.CreateGameDeniedNameTooLong"));
            }

            if (!accessManager.isGameAllowed(romName)) {
                log.warn(user + " create game denied: AccessManager denied game: " + romName);
                throw new CreateGameException(
                        EmuLang.getString("KailleraServerImpl.CreateGameDeniedGameBanned"));
            }
        }

        int gameID = gameManager.getNextGameID();
        KailleraGameImpl game = new KailleraGameImpl(gameID, romName, (KailleraUserImpl) user, this,
                gameBufferSize, gameTimeoutMillis, gameDesynchTimeouts);
        gameManager.addGame(game);

        addEvent(new GameCreatedEvent(this, game));

        log.info(user + " created: " + game + ": " + game.getRomName());

        try {
            user.joinGame(game.getID());
        } catch (Exception e) {
            // this shouldn't happen
            log.error("Caught exception while making owner join game! This shouldn't happen!", e);
        }

        announce(EmuLang.getString("KailleraServerImpl.UserCreatedGameAnnouncement", user.getName(),
                game.getRomName()), false);

        return game;
    }

    synchronized void closeGame(KailleraGame game, KailleraUser user) throws CloseGameException {
        if (!user.isLoggedIn()) {
            log.error(user + " close " + game + " failed: Not logged in");
            throw new CloseGameException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
        }

        if (!gameManager.containsGame(game.getID())) {
            log.error(user + " close " + game + " failed: not in list: " + game);
            return;
        }

        ((KailleraGameImpl) game).close(user);
        gameManager.removeGame(game.getID());

        log.info(user + " closed: " + game);
        addEvent(new GameClosedEvent(this, game));
    }

    public void announce(String announcement, boolean gamesAlso) {
        announcementService.announce(announcement, gamesAlso, getUsers(), getGames());
    }

    void addEvent(ServerEvent event) {
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
            maintenanceTask = new ServerMaintenanceTask(userManager, accessManager, maxPing,
                    keepAliveTimeout, idleTimeout, this::handleQuitRequest);
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
