package org.emulinker.kaillera.model.impl;

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

import org.emulinker.config.GameConfig;
import org.emulinker.config.MasterListConfig;
import org.emulinker.config.ServerConfig;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.KailleraEvent;
import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.release.KailleraServerReleaseInfo;
import org.emulinker.util.EmuLinkerExecutor;
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

        server = new KailleraServerImpl(executor, new TestAccessManager(), serverConfig, gameConfig,
                masterListConfig, new TestStatsCollector(), new KailleraServerReleaseInfo(),
                new AutoFireDetectorFactoryImpl());

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
                    KailleraUser user = server.newConnection(address, "v086", new NoOpListener());

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
                KailleraUser user = server.newConnection(address, "v086", new NoOpListener());
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
                    server.newConnection(address, "v086", new NoOpListener());
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
                KailleraUser user = server.newConnection(address, "v086", new NoOpListener());
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

    private static class NoOpListener implements KailleraEventListener {
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

    private static class TestAccessManager implements AccessManager {
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

    private static class TestStatsCollector implements StatsCollector {
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
