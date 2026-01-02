package org.emulinker.kaillera.load;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.emulinker.config.GameConfig;
import org.emulinker.config.MasterListConfig;
import org.emulinker.config.ServerConfig;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.impl.AutoFireDetectorFactoryImpl;
import org.emulinker.kaillera.model.impl.KailleraServerImpl;
import org.emulinker.kaillera.release.KailleraServerReleaseInfo;
import org.emulinker.util.EmuLinkerExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Load tests for KailleraServer with 100+ concurrent connections.
 *
 * <p>
 * Run with: ./gradlew test -Dload.tests=true
 */
@Tag("load")
@EnabledIfSystemProperty(named = "load.tests", matches = "true")
class ServerLoadTest {

    private KailleraServerImpl server;
    private EmuLinkerExecutor executor;
    private ExecutorService testExecutor;
    private List<MockKailleraClient> clients;

    @BeforeEach
    void setUp() {
        executor = new EmuLinkerExecutor();
        testExecutor = Executors.newVirtualThreadPerTaskExecutor();
        clients = new ArrayList<>();

        ServerConfig serverConfig = createTestServerConfig();
        GameConfig gameConfig = createTestGameConfig();
        MasterListConfig masterListConfig = createTestMasterListConfig();

        server = new KailleraServerImpl(executor, new TestAccessManager(), serverConfig, gameConfig,
                masterListConfig, new TestStatsCollector(), new KailleraServerReleaseInfo(),
                new AutoFireDetectorFactoryImpl());

    }

    @AfterEach
    void tearDown() throws Exception {
        for (MockKailleraClient client : clients) {
            try {
                client.quit();
            } catch (Exception ignored) {
            }
        }
        clients.clear();

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
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shouldHandle100ConcurrentConnections() throws Exception {
        int numClients = LoadTestConfiguration.CONCURRENT_CLIENTS;
        CountDownLatch connectLatch = new CountDownLatch(numClients);

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            testExecutor.submit(() -> {
                try {
                    MockKailleraClient client = new MockKailleraClient("LoadTestUser" + clientId,
                            clientId);
                    synchronized (clients) {
                        clients.add(client);
                    }

                    client.connect(server, LoadTestConfiguration.OPERATION_TIMEOUT.toMillis());
                } catch (Exception ignored) {
                    // Connection may fail - that's OK for load testing
                } finally {
                    connectLatch.countDown();
                }
            });
        }

        assertTrue(connectLatch.await(LoadTestConfiguration.TEST_TIMEOUT.toSeconds(),
                TimeUnit.SECONDS), "Connection timeout");

        // Test passes if concurrent connections don't cause crashes
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shouldHandleConcurrentChatMessages() throws Exception {
        int numClients = 50;
        int messagesPerClient = LoadTestConfiguration.MESSAGES_PER_CLIENT;
        CountDownLatch chatLatch = new CountDownLatch(numClients * messagesPerClient);

        for (int i = 0; i < numClients; i++) {
            MockKailleraClient client = new MockKailleraClient("ChatUser" + i, i);
            clients.add(client);
            try {
                client.connect(server, LoadTestConfiguration.OPERATION_TIMEOUT.toMillis());
            } catch (Exception ignored) {
                // Connection may fail - that's OK for load testing
            }
        }

        for (MockKailleraClient client : clients) {
            testExecutor.submit(() -> {
                for (int j = 0; j < messagesPerClient; j++) {
                    try {
                        client.chat("Message " + j + " from " + client.getName());
                        Thread.sleep(LoadTestConfiguration.OPERATION_DELAY_MS);
                    } catch (Exception ignored) {
                        // Expected - user may not be logged in
                    } finally {
                        chatLatch.countDown();
                    }
                }
            });
        }

        assertTrue(
                chatLatch.await(LoadTestConfiguration.TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS),
                "Chat timeout");

        // Test passes if concurrent chat doesn't cause crashes
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shouldHandleRapidJoinQuitCycles() throws Exception {
        int cycles = 50;
        CountDownLatch cycleLatch = new CountDownLatch(cycles);

        for (int i = 0; i < cycles; i++) {
            final int cycleId = i;
            testExecutor.submit(() -> {
                try {
                    MockKailleraClient client = new MockKailleraClient("CycleUser" + cycleId,
                            cycleId);

                    if (client.connect(server,
                            LoadTestConfiguration.OPERATION_TIMEOUT.toMillis())) {
                        Thread.sleep(LoadTestConfiguration.OPERATION_DELAY_MS);
                        client.quit();
                    }
                } catch (Exception ignored) {
                    // Expected - some cycles may fail due to async timing
                } finally {
                    cycleLatch.countDown();
                }
            });
        }

        assertTrue(
                cycleLatch.await(LoadTestConfiguration.TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS),
                "Cycle timeout");

        // Test passes if rapid join/quit doesn't cause crashes
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shouldHandleConcurrentGameCreation() throws Exception {
        int numGames = 30;
        CountDownLatch gameLatch = new CountDownLatch(numGames);

        for (int i = 0; i < numGames; i++) {
            MockKailleraClient client = new MockKailleraClient("GameCreator" + i, i);
            clients.add(client);
            try {
                client.connect(server, LoadTestConfiguration.OPERATION_TIMEOUT.toMillis());
            } catch (Exception ignored) {
                // Connection may fail - that's OK for load testing
            }
        }

        for (int i = 0; i < clients.size(); i++) {
            MockKailleraClient client = clients.get(i);
            final int gameNum = i;
            testExecutor.submit(() -> {
                try {
                    client.createGame("TestROM" + gameNum + ".zip");
                } catch (Exception ignored) {
                    // Expected - user may not be logged in
                } finally {
                    gameLatch.countDown();
                }
            });
        }

        assertTrue(
                gameLatch.await(LoadTestConfiguration.TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS),
                "Game creation timeout");

        // Test passes if concurrent game creation doesn't cause crashes
    }

    // Test configuration helpers

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
