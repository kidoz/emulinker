package org.emulinker.kaillera.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.emulinker.config.ControllersConfig;
import org.emulinker.config.GameConfig;
import org.emulinker.config.MasterListConfig;
import org.emulinker.config.ServerConfig;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.action.ACKAction;
import org.emulinker.kaillera.controller.v086.action.ActionBundle;
import org.emulinker.kaillera.controller.v086.action.ActionRouter;
import org.emulinker.kaillera.controller.v086.action.AdminCommandAction;
import org.emulinker.kaillera.controller.v086.action.CachedGameDataAction;
import org.emulinker.kaillera.controller.v086.action.ChatAction;
import org.emulinker.kaillera.controller.v086.action.CloseGameAction;
import org.emulinker.kaillera.controller.v086.action.CreateGameAction;
import org.emulinker.kaillera.controller.v086.action.DropGameAction;
import org.emulinker.kaillera.controller.v086.action.GameChatAction;
import org.emulinker.kaillera.controller.v086.action.GameDataAction;
import org.emulinker.kaillera.controller.v086.action.GameDesynchAction;
import org.emulinker.kaillera.controller.v086.action.GameInfoAction;
import org.emulinker.kaillera.controller.v086.action.GameKickAction;
import org.emulinker.kaillera.controller.v086.action.GameOwnerCommandAction;
import org.emulinker.kaillera.controller.v086.action.GameStatusAction;
import org.emulinker.kaillera.controller.v086.action.GameTimeoutAction;
import org.emulinker.kaillera.controller.v086.action.InfoMessageAction;
import org.emulinker.kaillera.controller.v086.action.JoinGameAction;
import org.emulinker.kaillera.controller.v086.action.KeepAliveAction;
import org.emulinker.kaillera.controller.v086.action.LoginAction;
import org.emulinker.kaillera.controller.v086.action.LoginProgressAction;
import org.emulinker.kaillera.controller.v086.action.PlayerDesynchAction;
import org.emulinker.kaillera.controller.v086.action.QuitAction;
import org.emulinker.kaillera.controller.v086.action.QuitGameAction;
import org.emulinker.kaillera.controller.v086.action.StartGameAction;
import org.emulinker.kaillera.controller.v086.action.UserReadyAction;
import org.emulinker.kaillera.load.UdpKailleraClient;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.impl.AutoFireDetectorFactoryImpl;
import org.emulinker.kaillera.model.impl.KailleraServerImpl;
import org.emulinker.kaillera.release.KailleraServerReleaseInfo;

import su.kidoz.kaillera.model.impl.GameManager;
import su.kidoz.kaillera.model.impl.UserManager;
import su.kidoz.kaillera.model.validation.LoginValidator;
import su.kidoz.kaillera.service.AnnouncementService;
import su.kidoz.kaillera.service.ChatModerationService;
import org.emulinker.util.EmuLinkerExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * End-to-end protocol tests that verify the Kaillera UDP protocol works
 * correctly.
 *
 * <p>
 * These tests start an embedded server with the full controller stack and use
 * {@link UdpKailleraClient} to test the actual UDP protocol communication.
 */
class ProtocolE2ETest {

    private static final Logger log = LoggerFactory.getLogger(ProtocolE2ETest.class);

    private static final int TEST_PORT = 27788; // Use non-standard port to avoid conflicts
    private static final long TIMEOUT_MS = 5000;

    private EmuLinkerExecutor executor;
    private KailleraServerImpl server;
    private V086Controller v086Controller;
    private ConnectController connectController;

    @BeforeEach
    void setUp() throws Exception {
        executor = new EmuLinkerExecutor();

        // Create configurations
        ServerConfig serverConfig = createServerConfig();
        GameConfig gameConfig = createGameConfig();
        MasterListConfig masterListConfig = createMasterListConfig();
        ControllersConfig controllersConfig = createControllersConfig();

        // Create dependencies for server
        TestAccessManager accessManager = new TestAccessManager();
        LoginValidator loginValidator = new LoginValidator(accessManager, serverConfig);
        ChatModerationService chatModerationService = new ChatModerationService(accessManager,
                serverConfig.getChatFloodTime(), serverConfig.getMaxChatLength());
        AnnouncementService announcementService = new AnnouncementService();
        UserManager userManager = new UserManager(serverConfig.getMaxUsers());
        GameManager gameManager = new GameManager(serverConfig.getMaxUsers());

        // Create server
        server = new KailleraServerImpl(executor, accessManager, serverConfig, gameConfig,
                masterListConfig, new TestStatsCollector(), new KailleraServerReleaseInfo(),
                new AutoFireDetectorFactoryImpl(), loginValidator, chatModerationService,
                announcementService, userManager, gameManager);
        server.start();

        // Create action router
        ActionRouter actionRouter = createActionRouter();

        // Create V086 controller
        v086Controller = new V086Controller(server, executor, controllersConfig, serverConfig,
                actionRouter);
        v086Controller.start();

        // Create connect controller
        KailleraServerController[] controllers = new KailleraServerController[]{v086Controller};
        connectController = new ConnectController(executor, controllers, new TestAccessManager(),
                controllersConfig);

        log.info("E2E test server started on port {}", TEST_PORT);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connectController != null) {
            connectController.stop();
        }
        if (v086Controller != null) {
            v086Controller.stop();
        }
        if (server != null) {
            server.stop();
        }
        if (executor != null) {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        log.info("E2E test server stopped");
    }

    @Test
    @Timeout(30)
    @DisplayName("Should complete HELLO handshake and receive assigned port")
    void shouldCompleteHelloHandshake() throws Exception {
        UdpKailleraClient client = new UdpKailleraClient("localhost", TEST_PORT, "E2ETestUser1");

        try {
            boolean connected = client.connect(TIMEOUT_MS);

            assertTrue(connected, "Client should connect successfully");
            assertTrue(client.isConnected(), "Client should be in connected state");
            assertTrue(client.getAssignedPort() > 0, "Should receive assigned port");

            log.info("Client connected, assigned port: {}", client.getAssignedPort());
        } finally {
            client.close();
        }
    }

    @Test
    @Timeout(30)
    @DisplayName("Should complete full login flow")
    void shouldCompleteLoginFlow() throws Exception {
        UdpKailleraClient client = new UdpKailleraClient("localhost", TEST_PORT, "E2ETestUser2");

        try {
            // Connect
            boolean connected = client.connect(TIMEOUT_MS);
            assertTrue(connected, "Client should connect");

            // Login
            boolean loggedIn = client.login(TIMEOUT_MS);
            assertTrue(loggedIn, "Client should login successfully");
            assertTrue(client.isLoggedIn(), "Client should be in logged-in state");

            // Verify server state
            assertEquals(1, server.getNumUsers(), "Server should have 1 user");

            log.info("Client logged in successfully");
        } finally {
            client.close();
        }
    }

    @Test
    @Timeout(30)
    @DisplayName("Should send and receive chat messages")
    void shouldSendChatMessages() throws Exception {
        UdpKailleraClient client = new UdpKailleraClient("localhost", TEST_PORT, "E2EChatUser");

        try {
            // Connect and login
            assertTrue(client.connect(TIMEOUT_MS), "Client should connect");
            assertTrue(client.login(TIMEOUT_MS), "Client should login");

            // Send chat message (no exception = success)
            client.chat("Hello from E2E test!");

            // Small delay to let message process
            Thread.sleep(100);

            log.info("Chat message sent successfully");
        } finally {
            client.close();
        }
    }

    @Test
    @Timeout(30)
    @DisplayName("Should quit gracefully")
    void shouldQuitGracefully() throws Exception {
        UdpKailleraClient client = new UdpKailleraClient("localhost", TEST_PORT, "E2EQuitUser");

        try {
            // Connect and login
            assertTrue(client.connect(TIMEOUT_MS), "Client should connect");
            assertTrue(client.login(TIMEOUT_MS), "Client should login");
            assertEquals(1, server.getNumUsers(), "Server should have 1 user");

            // Quit
            client.quit("Goodbye!");

            // Wait for quit to process
            Thread.sleep(500);

            // Verify user was removed
            assertEquals(0, server.getNumUsers(), "Server should have 0 users after quit");

            log.info("Client quit gracefully");
        } finally {
            client.close();
        }
    }

    @Test
    @Timeout(30)
    @DisplayName("Should handle multiple concurrent clients")
    void shouldHandleMultipleConcurrentClients() throws Exception {
        int numClients = 5;
        UdpKailleraClient[] clients = new UdpKailleraClient[numClients];

        try {
            // Connect all clients
            for (int i = 0; i < numClients; i++) {
                clients[i] = new UdpKailleraClient("localhost", TEST_PORT, "E2EMultiUser" + i);
                assertTrue(clients[i].connect(TIMEOUT_MS), "Client " + i + " should connect");
                assertTrue(clients[i].login(TIMEOUT_MS), "Client " + i + " should login");

                // Small delay between connections
                Thread.sleep(100);
            }

            // Verify all clients are connected
            assertEquals(numClients, server.getNumUsers(),
                    "Server should have " + numClients + " users");

            log.info("All {} clients connected and logged in", numClients);

            // Each client sends a chat message
            for (int i = 0; i < numClients; i++) {
                clients[i].chat("Message from client " + i);
            }

            Thread.sleep(200);

            // Disconnect all clients
            for (int i = 0; i < numClients; i++) {
                clients[i].quit("Bye from client " + i);
            }

            Thread.sleep(500);

            assertEquals(0, server.getNumUsers(), "Server should have 0 users after all quit");
        } finally {
            for (UdpKailleraClient client : clients) {
                if (client != null) {
                    try {
                        client.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Test
    @Timeout(30)
    @DisplayName("Should handle reconnection after disconnect")
    void shouldHandleReconnection() throws Exception {
        UdpKailleraClient client1 = new UdpKailleraClient("localhost", TEST_PORT,
                "E2EReconnectUser");

        try {
            // First connection
            assertTrue(client1.connect(TIMEOUT_MS), "First connect should succeed");
            assertTrue(client1.login(TIMEOUT_MS), "First login should succeed");
            assertEquals(1, server.getNumUsers(), "Server should have 1 user");

            // Disconnect
            client1.quit("First disconnect");
            Thread.sleep(500);
            assertEquals(0, server.getNumUsers(), "Server should have 0 users");
            client1.close();

            // Reconnect with new client
            UdpKailleraClient client2 = new UdpKailleraClient("localhost", TEST_PORT,
                    "E2EReconnectUser");
            try {
                assertTrue(client2.connect(TIMEOUT_MS), "Reconnect should succeed");
                assertTrue(client2.login(TIMEOUT_MS), "Reconnect login should succeed");
                assertEquals(1, server.getNumUsers(), "Server should have 1 user after reconnect");

                log.info("Reconnection successful");
            } finally {
                client2.close();
            }
        } finally {
            client1.close();
        }
    }

    // Configuration helpers

    private ServerConfig createServerConfig() {
        return new ServerConfig() {
            @Override
            public int getMaxPing() {
                return 1000;
            }

            @Override
            public int getMaxUsers() {
                return 100;
            }

            @Override
            public int getMaxGames() {
                return 50;
            }

            @Override
            public int getKeepAliveTimeout() {
                return 60000;
            }

            @Override
            public int getIdleTimeout() {
                return 60000;
            }

            @Override
            public int getChatFloodTime() {
                return 0;
            }

            @Override
            public int getCreateGameFloodTime() {
                return 0;
            }

            @Override
            public boolean isAllowSinglePlayer() {
                return true;
            }

            @Override
            public boolean isAllowMultipleConnections() {
                return true;
            }

            @Override
            public int getMaxUserNameLength() {
                return 31;
            }

            @Override
            public int getMaxChatLength() {
                return 127;
            }

            @Override
            public int getMaxGameNameLength() {
                return 127;
            }

            @Override
            public int getMaxQuitMessageLength() {
                return 127;
            }

            @Override
            public int getMaxClientNameLength() {
                return 127;
            }
        };
    }

    private GameConfig createGameConfig() {
        return new GameConfig() {
            @Override
            public int getBufferSize() {
                return 100;
            }

            @Override
            public int getTimeoutMillis() {
                return 5000;
            }

            @Override
            public int getDesynchTimeouts() {
                return 4;
            }

            @Override
            public int getDefaultAutoFireSensitivity() {
                return 0;
            }
        };
    }

    private MasterListConfig createMasterListConfig() {
        MasterListConfig config = new MasterListConfig();
        config.setTouchKaillera(false);
        config.setTouchEmulinker(false);
        return config;
    }

    private ControllersConfig createControllersConfig() {
        ControllersConfig config = new ControllersConfig();

        ControllersConfig.Connect connectConfig = new ControllersConfig.Connect();
        connectConfig.setPort(TEST_PORT);
        connectConfig.setBufferSize(4096);
        config.setConnect(connectConfig);

        ControllersConfig.V086 v086Config = new ControllersConfig.V086();
        v086Config.setPortRangeStart(TEST_PORT + 1);
        v086Config.setBufferSize(4096);
        config.setV086(v086Config);

        config.setBindAddresses(List.of("0.0.0.0"));

        return config;
    }

    private ActionRouter createActionRouter() {
        AdminCommandAction adminCommandAction = new AdminCommandAction();
        GameOwnerCommandAction gameOwnerCommandAction = new GameOwnerCommandAction();

        ActionBundle bundle = new ActionBundle(new ACKAction(), adminCommandAction,
                new CachedGameDataAction(), new ChatAction(adminCommandAction),
                new CloseGameAction(), new CreateGameAction(), new DropGameAction(),
                new GameChatAction(gameOwnerCommandAction), new GameDataAction(),
                new GameDesynchAction(), new GameInfoAction(), new GameKickAction(),
                gameOwnerCommandAction, new GameStatusAction(), new GameTimeoutAction(),
                new InfoMessageAction(), new JoinGameAction(), new KeepAliveAction(),
                new LoginAction(), new LoginProgressAction(), new PlayerDesynchAction(),
                new QuitAction(), new QuitGameAction(), new StartGameAction(),
                new UserReadyAction());

        return new ActionRouter(bundle);
    }

    private static final class TestAccessManager implements AccessManager {
        @Override
        public boolean isAddressAllowed(InetAddress address) {
            return true;
        }

        @Override
        public boolean isSilenced(InetAddress address) {
            return false;
        }

        @Override
        public boolean isEmulatorAllowed(String emulator) {
            return true;
        }

        @Override
        public boolean isGameAllowed(String game) {
            return true;
        }

        @Override
        public int getAccess(InetAddress address) {
            return ACCESS_NORMAL;
        }

        @Override
        public String getAnnouncement(InetAddress address) {
            return null;
        }

        @Override
        public void addTempBan(String pattern, int minutes) {
        }

        @Override
        public void addTempAdmin(String pattern, int minutes) {
        }

        @Override
        public void addSilenced(String pattern, int minutes) {
        }

        @Override
        public boolean clearTemp(InetAddress address) {
            return true;
        }
    }

    private static final class TestStatsCollector implements StatsCollector {
        @Override
        public void gameStarted(KailleraServer server, KailleraGame game) {
        }

        @Override
        public List<String> getStartedGamesList() {
            return List.of();
        }

        @Override
        public void clearStartedGamesList() {
        }
    }
}
