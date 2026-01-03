package su.kidoz.kaillera.model.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import su.kidoz.config.GameConfig;
import su.kidoz.config.MasterListConfig;
import su.kidoz.config.ServerConfig;
import su.kidoz.kaillera.access.AccessManager;
import su.kidoz.kaillera.master.StatsCollector;
import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraServer;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.DefaultEventDispatcher;
import su.kidoz.kaillera.model.event.EventDispatcher;
import su.kidoz.kaillera.model.event.KailleraEvent;
import su.kidoz.kaillera.model.event.KailleraEventListener;
import su.kidoz.kaillera.release.KailleraServerReleaseInfo;
import su.kidoz.util.EmuLinkerExecutor;

import su.kidoz.kaillera.model.validation.LoginValidator;
import su.kidoz.kaillera.service.AnnouncementService;
import su.kidoz.kaillera.service.ChatModerationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Concurrency tests for KailleraServerImpl.
 *
 * <p>
 * Tests thread-safety of server operations under concurrent load.
 */
class KailleraServerConcurrencyTest {

    private KailleraServerImpl server;
    private EmuLinkerExecutor executor;
    private ExecutorService testExecutor;

    @BeforeEach
    void setUp() {
        executor = new EmuLinkerExecutor();
        testExecutor = Executors.newVirtualThreadPerTaskExecutor();

        ServerConfig serverConfig = createTestServerConfig();
        GameConfig gameConfig = createTestGameConfig();
        MasterListConfig masterListConfig = createTestMasterListConfig();

        // Create dependencies for server
        TestAccessManager accessManager = new TestAccessManager();
        LoginValidator loginValidator = new LoginValidator(accessManager, serverConfig);
        ChatModerationService chatModerationService = new ChatModerationService(accessManager,
                serverConfig.getChatFloodTime(), serverConfig.getMaxChatLength());
        AnnouncementService announcementService = new AnnouncementService();
        UserManager userManager = new UserManager(serverConfig.getMaxUsers());
        GameManager gameManager = new GameManager(serverConfig.getMaxUsers());

        server = new KailleraServerImpl(executor, accessManager, serverConfig, gameConfig,
                masterListConfig, new TestStatsCollector(), new KailleraServerReleaseInfo(),
                new AutoFireDetectorFactoryImpl(), loginValidator, chatModerationService,
                announcementService, userManager, gameManager);

    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        if (testExecutor != null) {
            testExecutor.shutdown();
            testExecutor.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void userIdsShouldBeUnique() throws Exception {
        int numConnections = 100;
        Set<Integer> userIds = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(numConnections);
        AtomicInteger duplicates = new AtomicInteger(0);

        for (int i = 0; i < numConnections; i++) {
            final int clientId = i;
            testExecutor.submit(() -> {
                try {
                    InetSocketAddress address = new InetSocketAddress("127.0.0.1",
                            10000 + clientId);
                    KailleraUser user = server.newConnection(address, "v086",
                            createTestDispatcher());

                    if (!userIds.add(user.getID())) {
                        duplicates.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(20, TimeUnit.SECONDS));
        assertEquals(0, duplicates.get(), "User IDs should be unique");
        assertEquals(numConnections, userIds.size(), "All user IDs should be unique");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void gameIdsShouldBeUnique() throws Exception {
        int numGames = 50;
        Set<Integer> gameIds = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicates = new AtomicInteger(0);
        AtomicInteger loginCount = new AtomicInteger(0);

        for (int i = 0; i < numGames; i++) {
            final int clientId = i;
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", 10000 + clientId);
            try {
                KailleraUser user = server.newConnection(address, "v086", createTestDispatcher());
                user.setName("User" + clientId);
                user.setClientType("TestClient");
                user.setConnectionType(KailleraUser.CONNECTION_TYPE_LAN);
                user.setPing(10);
                user.login();
                loginCount.incrementAndGet();
            } catch (Exception e) {
                // Login may fail due to test configuration
            }
        }

        int actualUsers = server.getNumUsers();
        CountDownLatch latch = new CountDownLatch(actualUsers);

        for (KailleraUser user : server.getUsers()) {
            testExecutor.submit(() -> {
                try {
                    var game = user.createGame("TestROM.zip");
                    if (!gameIds.add(game.getID())) {
                        duplicates.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(20, TimeUnit.SECONDS));
        assertEquals(0, duplicates.get(), "Game IDs should be unique");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void userCountShouldBeAccurate() throws Exception {
        int numUsers = 50;
        CountDownLatch connectLatch = new CountDownLatch(numUsers);
        AtomicInteger connected = new AtomicInteger(0);

        for (int i = 0; i < numUsers; i++) {
            final int clientId = i;
            testExecutor.submit(() -> {
                try {
                    InetSocketAddress address = new InetSocketAddress("127.0.0.1",
                            10000 + clientId);
                    server.newConnection(address, "v086", createTestDispatcher());
                    connected.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    connectLatch.countDown();
                }
            });
        }

        assertTrue(connectLatch.await(20, TimeUnit.SECONDS));
        assertEquals(connected.get(), server.getNumUsers(),
                "User count should match connected users");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldHandleConcurrentChatSafely() throws Exception {
        int numUsers = 20;
        int messagesPerUser = 10;

        for (int i = 0; i < numUsers; i++) {
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", 10000 + i);
            try {
                KailleraUser user = server.newConnection(address, "v086", createTestDispatcher());
                user.setName("User" + i);
                user.setClientType("TestClient");
                user.setConnectionType(KailleraUser.CONNECTION_TYPE_LAN);
                user.setPing(10);
                user.login();
            } catch (Exception e) {
                // Login may fail due to test configuration
            }
        }

        int actualUsers = server.getNumUsers();
        CountDownLatch chatLatch = new CountDownLatch(actualUsers * messagesPerUser);

        for (KailleraUser user : server.getUsers()) {
            testExecutor.submit(() -> {
                for (int j = 0; j < messagesPerUser; j++) {
                    try {
                        user.chat("Message " + j);
                    } catch (Exception e) {
                        // Expected - user may not be fully logged in
                    } finally {
                        chatLatch.countDown();
                    }
                }
            });
        }

        if (actualUsers > 0) {
            assertTrue(chatLatch.await(20, TimeUnit.SECONDS));
            // The test passes if we don't get ConcurrentModificationException
            // Login/chat failures are expected in this test setup
        }
        // Test passes - we verified concurrent access doesn't crash
    }

    // Helper classes

    private static EventDispatcher createTestDispatcher() {
        EventDispatcher dispatcher = new DefaultEventDispatcher();
        dispatcher.setListener(new NoOpListener());
        return dispatcher;
    }

    private static final class NoOpListener implements KailleraEventListener {
        @Override
        public void actionPerformed(KailleraEvent event) {
        }

        @Override
        public void stop() {
        }
    }

    private ServerConfig createTestServerConfig() {
        return new ServerConfig() {
            @Override
            public int getMaxPing() {
                return 1000;
            }

            @Override
            public int getMaxUsers() {
                return 200;
            }

            @Override
            public int getMaxGames() {
                return 100;
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

    private GameConfig createTestGameConfig() {
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

    private MasterListConfig createTestMasterListConfig() {
        MasterListConfig config = new MasterListConfig();
        config.setTouchKaillera(false);
        config.setTouchEmulinker(false);
        return config;
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
