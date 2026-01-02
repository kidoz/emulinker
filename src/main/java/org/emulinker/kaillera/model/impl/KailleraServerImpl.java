package org.emulinker.kaillera.model.impl;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

public class KailleraServerImpl implements KailleraServer, Executable {
    protected static final Logger log = LoggerFactory.getLogger(KailleraServerImpl.class);

    protected final int maxPing;
    protected final int maxUsers;
    protected final int maxGames;
    protected final int idleTimeout;
    protected final int keepAliveTimeout;
    protected final int chatFloodTime;
    protected final int createGameFloodTime;
    protected final int maxUserNameLength;
    protected final int maxChatLength;
    protected final int maxGameNameLength;
    protected final int maxQuitMessageLength;
    protected final int maxClientNameLength;

    protected final int gameBufferSize;
    protected final int gameTimeoutMillis;
    protected final int gameDesynchTimeouts;
    protected final int gameAutoFireSensitivity;

    protected final ServerConfig serverConfig;

    protected final List<String> loginMessages = new ArrayList<String>();
    protected final boolean allowSinglePlayer;
    protected final boolean allowMultipleConnections;

    protected volatile boolean stopFlag = false;
    protected volatile boolean isRunning = false;

    protected final AtomicInteger connectionCounter = new AtomicInteger(1);
    protected final AtomicInteger gameCounter = new AtomicInteger(1);

    protected final EmuLinkerExecutor threadPool;
    protected final AccessManager accessManager;
    protected StatsCollector statsCollector;
    protected final ReleaseInfo releaseInfo;
    protected final AutoFireDetectorFactory autoFireDetectorFactory;

    protected final Map<Integer, KailleraUserImpl> users;
    protected final Map<Integer, KailleraGameImpl> games;

    public KailleraServerImpl(EmuLinkerExecutor threadPool, AccessManager accessManager,
            ServerConfig serverConfig, GameConfig gameConfig, MasterListConfig masterListConfig,
            StatsCollector statsCollector, ReleaseInfo releaseInfo,
            AutoFireDetectorFactory autoFireDetectorFactory) {
        this.threadPool = threadPool;
        this.accessManager = accessManager;
        this.releaseInfo = releaseInfo;
        this.autoFireDetectorFactory = autoFireDetectorFactory;
        this.serverConfig = serverConfig;

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

        users = new ConcurrentHashMap<Integer, KailleraUserImpl>(maxUsers);
        games = new ConcurrentHashMap<Integer, KailleraGameImpl>(maxUsers);

        // Master list config
        if (masterListConfig.isTouchKaillera())
            this.statsCollector = statsCollector;
    }

    public AccessManager getAccessManager() {
        return accessManager;
    }

    public KailleraUser getUser(int userID) {
        return users.get(userID);
    }

    public KailleraGame getGame(int gameID) {
        return games.get(gameID);
    }

    public Collection<KailleraUserImpl> getUsers() {
        return Collections.unmodifiableCollection(users.values());
    }

    public Collection<KailleraGameImpl> getGames() {
        return Collections.unmodifiableCollection(games.values());
    }

    public int getNumUsers() {
        return users.size();
    }

    public int getNumGames() {
        return games.size();
    }

    public int getNumGamesPlaying() {
        int count = 0;
        for (KailleraGameImpl game : getGames()) {
            if (game.getStatus() != KailleraGame.STATUS_WAITING)
                count++;
        }
        return count;
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

    protected int getChatFloodTime() {
        return chatFloodTime;
    }

    protected int getCreateGameFloodTime() {
        return createGameFloodTime;
    }

    protected boolean getAllowSinglePlayer() {
        return allowSinglePlayer;
    }

    protected int getMaxUserNameLength() {
        return maxUserNameLength;
    }

    protected int getMaxChatLength() {
        return maxChatLength;
    }

    protected int getMaxGameNameLength() {
        return maxGameNameLength;
    }

    protected int getQuitMessageLength() {
        return maxQuitMessageLength;
    }

    protected int getMaxClientNameLength() {
        return maxClientNameLength;
    }

    protected boolean getAllowMultipleConnections() {
        return allowMultipleConnections;
    }

    public EmuLinkerExecutor getThreadPool() {
        return threadPool;
    }

    public String toString() {
        return "KailleraServerImpl[numUsers=" + getNumUsers() + " numGames=" + getNumGames()
                + " isRunning=" + isRunning() + "]"; //$NON-NLS-2$
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

        for (KailleraUserImpl user : users.values())
            user.stop();

        users.clear();
        games.clear();
    }

    protected int getNextUserID() {
        return connectionCounter.getAndUpdate(val -> val >= 0xFFFF ? 1 : val + 1);
    }

    protected int getNextGameID() {
        return gameCounter.getAndUpdate(val -> val >= 0xFFFF ? 1 : val + 1);
    }

    protected StatsCollector getStatsCollector() {
        return statsCollector;
    }

    protected AutoFireDetector getAutoFireDetector(KailleraGame game) {
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
        if (getMaxUsers() > 0 && users.size() >= getMaxUsers()
                && !(access > AccessManager.ACCESS_NORMAL)) {
            log.warn("Connection from " + EmuUtil.formatSocketAddress(clientSocketAddress)
                    + " denied: Server is full!");
            throw new ServerFullException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedServerFull"));
        }

        int userID = getNextUserID();
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
        users.put(userID, user);

        return user;
    }

    public synchronized void login(KailleraUser user) throws PingTimeException,
            ClientAddressException, ConnectionTypeException, UserNameException, LoginException {
        KailleraUserImpl userImpl = (KailleraUserImpl) user;

        long loginDelay = (System.currentTimeMillis() - user.getConnectTime());
        log.info(user + ": login request: delay=" + loginDelay + "ms, clientAddress="
                + EmuUtil.formatSocketAddress(user.getSocketAddress()) + ", name=" + user.getName()
                + ", ping=" //$NON-NLS-1$
                + user.getPing() + ", client=" + user.getClientType() + ", connection=" //$NON-NLS-1$
                + KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]);

        if (user.isLoggedIn()) {
            log.warn(user + " login denied: Already logged in!");
            throw new LoginException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedAlreadyLoggedIn"));
        }

        Integer userListKey = Integer.valueOf(user.getID());
        KailleraUser u = users.get(userListKey);
        if (u == null) {
            log.warn(user + " login denied: Connection timed out!");
            throw new LoginException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedConnectionTimedOut"));
        }

        int access = accessManager.getAccess(user.getSocketAddress().getAddress());
        if (access < AccessManager.ACCESS_NORMAL) {
            log.info(user + " login denied: Access denied");
            users.remove(userListKey);
            throw new LoginException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedAccessDenied"));
        }

        if (access == AccessManager.ACCESS_NORMAL && getMaxPing() > 0
                && user.getPing() > getMaxPing()) {
            log.info(user + " login denied: Ping " + user.getPing() + " > " + getMaxPing());
            users.remove(userListKey);
            throw new PingTimeException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedPingTooHigh",
                            (user.getPing() + " > " + getMaxPing())));
        }

        if (access == AccessManager.ACCESS_NORMAL
                && !serverConfig.isConnectionTypeAllowed(user.getConnectionType())) {
            log.info(user + " login denied: Connection "
                    + KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]
                    + " Not Allowed");
            users.remove(userListKey);
            throw new LoginException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedConnectionTypeDenied",
                            KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]));
        }

        if (user.getPing() < 0) {
            log.warn(user + " login denied: Invalid ping: " + user.getPing());
            users.remove(userListKey);
            throw new PingTimeException(
                    EmuLang.getString("KailleraServerImpl.LoginErrorInvalidPing", user.getPing()));
        }

        if (access == AccessManager.ACCESS_NORMAL && user.getName().trim().length() == 0) {
            log.info(user + " login denied: Empty UserName");
            users.remove(userListKey);
            throw new UserNameException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedUserNameEmpty"));
        }

        if (access == AccessManager.ACCESS_NORMAL && maxUserNameLength > 0
                && user.getName().length() > getMaxUserNameLength()) {
            log.info(user + " login denied: UserName Length > " + getMaxUserNameLength());
            users.remove(userListKey);
            throw new UserNameException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedUserNameTooLong"));
        }

        if (access == AccessManager.ACCESS_NORMAL && maxClientNameLength > 0
                && user.getClientType().length() > getMaxClientNameLength()) {
            log.info(user + " login denied: Client Name Length > " + getMaxClientNameLength());
            users.remove(userListKey);
            throw new UserNameException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedEmulatorNameTooLong"));
        }

        if (access == AccessManager.ACCESS_NORMAL) {
            if (containsIllegalCharacters(user.getName())) {
                log.info(user + " login denied: Illegal characters in UserName");
                users.remove(userListKey);
                throw new UserNameException(EmuLang
                        .getString("KailleraServerImpl.LoginDeniedIllegalCharactersInUserName"));
            }
        }

        if (u.getStatus() != KailleraUser.STATUS_CONNECTING) {
            users.remove(userListKey);
            log.warn(user + " login denied: Invalid status="
                    + KailleraUser.STATUS_NAMES[u.getStatus()]);
            throw new LoginException(
                    EmuLang.getString("KailleraServerImpl.LoginErrorInvalidStatus", u.getStatus()));
        }

        if (!u.getConnectSocketAddress().getAddress()
                .equals(user.getSocketAddress().getAddress())) {
            users.remove(userListKey);
            log.warn(user + " login denied: Connect address does not match login address: "
                    + u.getConnectSocketAddress().getAddress().getHostAddress() + " != "
                    + user.getSocketAddress().getAddress().getHostAddress());
            throw new ClientAddressException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedAddressMatchError"));
        }

        if (access == AccessManager.ACCESS_NORMAL
                && !accessManager.isEmulatorAllowed(user.getClientType())) {
            log.info(
                    user + " login denied: AccessManager denied emulator: " + user.getClientType());
            users.remove(userListKey);
            throw new LoginException(EmuLang.getString(
                    "KailleraServerImpl.LoginDeniedEmulatorRestricted", user.getClientType()));
        }

        for (KailleraUserImpl u2 : getUsers()) {
            if (u2.isLoggedIn()) {
                if (!u2.equals(u)
                        && u.getConnectSocketAddress().getAddress()
                                .equals(u2.getConnectSocketAddress().getAddress())
                        && u.getName().equals(u2.getName())) {
                    // user is attempting to login more than once with the same name and address
                    // logoff the old user and login the new one

                    try {
                        quit(u2, EmuLang.getString("KailleraServerImpl.ForcedQuitReconnected"));
                    } catch (Exception e) {
                        log.error("Error forcing " + u2 + " quit for reconnect!", e);
                    }
                } else if (access == AccessManager.ACCESS_NORMAL && !u2.equals(u)
                        && u.getConnectSocketAddress().getAddress()
                                .equals(u2.getConnectSocketAddress().getAddress())
                        && !u.getName().equals(u2.getName()) && !allowMultipleConnections) {
                    users.remove(userListKey);
                    log.warn(user + " login denied: Address already logged in as " + u2.getName());
                    throw new ClientAddressException(EmuLang.getString(
                            "KailleraServerImpl.LoginDeniedAlreadyLoggedInAs", u2.getName()));
                }
            }
        }

        // passed all checks

        userImpl.setAccess(access);
        userImpl.setStatus(KailleraUser.STATUS_IDLE);
        userImpl.setLoggedIn();
        users.put(userListKey, userImpl);
        userImpl.addEvent(new ConnectedEvent(this, user));

        // Release lock before sleeping to allow other logins to proceed
        // The user is already fully logged in at this point
        sendLoginNotificationsAsync(userImpl, access);
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
            users.remove(user.getID());
            log.error(user + " quit failed: Not logged in");
            throw new QuitException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
        }

        if (users.remove(user.getID()) == null)
            log.error(user + " quit failed: not in user list");

        KailleraGameImpl userGame = ((KailleraUserImpl) user).getGame();
        if (userGame != null)
            user.quitGame();

        String quitMsg = message.trim();
        if (quitMsg.length() == 0
                || (maxQuitMessageLength > 0 && quitMsg.length() > maxQuitMessageLength))
            quitMsg = EmuLang.getString("KailleraServerImpl.StandardQuitMessage");

        log.info(user + " quit: " + quitMsg);

        UserQuitEvent quitEvent = new UserQuitEvent(this, user, quitMsg);

        addEvent(quitEvent);
        ((KailleraUserImpl) user).addEvent(quitEvent);
    }

    public synchronized void chat(KailleraUser user, String message)
            throws ChatException, FloodException {
        if (!user.isLoggedIn()) {
            log.error(user + " chat failed: Not logged in");
            throw new ChatException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
        }

        int access = accessManager.getAccess(user.getSocketAddress().getAddress());
        if (access == AccessManager.ACCESS_NORMAL
                && accessManager.isSilenced(user.getSocketAddress().getAddress())) {
            log.warn(user + " chat denied: Silenced: " + message);
            throw new ChatException(EmuLang.getString("KailleraServerImpl.ChatDeniedSilenced"));
        }

        if (access == AccessManager.ACCESS_NORMAL && chatFloodTime > 0
                && (System.currentTimeMillis()
                        - ((KailleraUserImpl) user).getLastChatTime()) < (chatFloodTime * 1000)) {
            log.warn(user + " chat denied: Flood: " + message);
            throw new FloodException(
                    EmuLang.getString("KailleraServerImpl.ChatDeniedFloodControl"));
        }

        message = message.trim();
        if (message.length() == 0)
            return;

        if (access == AccessManager.ACCESS_NORMAL) {
            if (containsIllegalCharacters(message)) {
                log.warn(user + " chat denied: Illegal characters in message");
                throw new ChatException(
                        EmuLang.getString("KailleraServerImpl.ChatDeniedIllegalCharacters"));
            }

            if (maxChatLength > 0 && message.length() > maxChatLength) {
                log.warn(user + " chat denied: Message Length > " + maxChatLength);
                throw new ChatException(
                        EmuLang.getString("KailleraServerImpl.ChatDeniedMessageTooLong"));
            }
        }

        log.info(user + " chat: " + message);
        addEvent(new ChatEvent(this, user, message));
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

            if (romName.trim().length() == 0) {
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

        KailleraGameImpl game = null;

        int gameID = getNextGameID();
        game = new KailleraGameImpl(gameID, romName, (KailleraUserImpl) user, this, gameBufferSize,
                gameTimeoutMillis, gameDesynchTimeouts);
        games.put(gameID, game);

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

        if (!games.containsKey(game.getID())) {
            log.error(user + " close " + game + " failed: not in list: " + game);
            return;
        }

        ((KailleraGameImpl) game).close(user);
        games.remove(game.getID());

        log.info(user + " closed: " + game);
        addEvent(new GameClosedEvent(this, game));
    }

    public void announce(String announcement, boolean gamesAlso) {
        for (KailleraUserImpl kailleraUser : getUsers()) {
            if (kailleraUser.isLoggedIn())
                kailleraUser.addEvent(new InfoMessageEvent(kailleraUser, announcement));
        }

        if (gamesAlso) {
            for (KailleraGameImpl kailleraGame : getGames()) {
                kailleraGame.announce(announcement);
            }
        }
    }

    protected void addEvent(ServerEvent event) {
        for (KailleraUserImpl user : users.values()) {
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
            while (!stopFlag) {
                try {
                    Thread.sleep((long) (maxPing * 3));
                } catch (InterruptedException e) {
                    log.error("Sleep Interrupted!", e);
                }

                // log.debug(this + " running maintenance...");

                if (stopFlag)
                    break;

                if (users.isEmpty())
                    continue;

                // Copy to avoid ConcurrentModificationException when removing during iteration
                List<KailleraUserImpl> usersSnapshot = new ArrayList<>(getUsers());
                List<Integer> usersToRemove = new ArrayList<>();

                for (KailleraUserImpl user : usersSnapshot) {
                    synchronized (user) {
                        int access = accessManager
                                .getAccess(user.getConnectSocketAddress().getAddress());
                        ((KailleraUserImpl) user).setAccess(access);

                        if (!user.isLoggedIn() && (System.currentTimeMillis()
                                - user.getConnectTime()) > (maxPing * 15)) {
                            log.info(user + " connection timeout!");
                            user.stop();
                            usersToRemove.add(user.getID());
                        } else if (user.isLoggedIn() && (System.currentTimeMillis()
                                - user.getLastKeepAlive()) > (keepAliveTimeout * 1000)) {
                            log.info(user + " keepalive timeout!");
                            try {
                                quit(user, EmuLang
                                        .getString("KailleraServerImpl.ForcedQuitPingTimeout"));
                            } catch (Exception e) {
                                log.error("Error forcing " + user + " quit for keepalive timeout!",
                                        e);
                            }
                        } else if (idleTimeout > 0 && access == AccessManager.ACCESS_NORMAL
                                && user.isLoggedIn() && (System.currentTimeMillis()
                                        - user.getLastActivity()) > (idleTimeout * 1000)) {
                            log.info(user + " inactivity timeout!");
                            try {
                                quit(user, EmuLang.getString(
                                        "KailleraServerImpl.ForcedQuitInactivityTimeout"));
                            } catch (Exception e) {
                                log.error("Error forcing " + user + " quit for inactivity timeout!",
                                        e);
                            }
                        } else if (user.isLoggedIn() && access < AccessManager.ACCESS_NORMAL) {
                            log.info(user + " banned!");
                            try {
                                quit(user,
                                        EmuLang.getString("KailleraServerImpl.ForcedQuitBanned"));
                            } catch (Exception e) {
                                log.error("Error forcing " + user + " quit because banned!", e);
                            }
                        } else if (user.isLoggedIn() && access == AccessManager.ACCESS_NORMAL
                                && !accessManager.isEmulatorAllowed(user.getClientType())) {
                            log.info(user + ": emulator restricted!");
                            try {
                                quit(user, EmuLang.getString(
                                        "KailleraServerImpl.ForcedQuitEmulatorRestricted"));
                            } catch (Exception e) {
                                log.error("Error forcing " + user
                                        + " quit because emulator restricted!", e);
                            }
                        }
                    }
                }

                // Remove users after iteration to avoid ConcurrentModificationException
                for (Integer userId : usersToRemove) {
                    users.remove(userId);
                }
            }
        } catch (Throwable e) {
            if (!stopFlag)
                log.error("KailleraServer thread caught unexpected exception: " + e, e);
        } finally {
            isRunning = false;
            log.debug("KailleraServer thread exiting...");
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
