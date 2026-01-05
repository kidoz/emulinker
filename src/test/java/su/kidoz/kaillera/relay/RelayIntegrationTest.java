package su.kidoz.kaillera.relay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import su.kidoz.config.RelayConfig;
import su.kidoz.util.EmuLinkerExecutor;

/**
 * Integration tests for relay mode controllers.
 *
 * <p>
 * Tests verify relay lifecycle, configuration, and basic message handling.
 */
@DisplayName("Relay Integration Tests")
class RelayIntegrationTest {

    private static final Charset CHARSET = Charset.forName("ISO-8859-1");
    private static final int TEST_LISTEN_PORT = 27887;
    private static final int TEST_BACKEND_PORT = 27888;
    private static final String TEST_BACKEND_HOST = "127.0.0.1";

    private EmuLinkerExecutor threadPool;
    private ScheduledExecutorService scheduler;
    private RelayConfig config;
    private KailleraRelayController relayController;

    @BeforeEach
    void setUp() {
        threadPool = new EmuLinkerExecutor();
        scheduler = Executors.newScheduledThreadPool(1);

        config = mock(RelayConfig.class);
        when(config.getListenPort()).thenReturn(TEST_LISTEN_PORT);
        when(config.getBackendHost()).thenReturn(TEST_BACKEND_HOST);
        when(config.getBackendPort()).thenReturn(TEST_BACKEND_PORT);
        when(config.getMaxConnections()).thenReturn(100);
        when(config.getBufferSize()).thenReturn(2048);
        when(config.getIdleTimeoutSeconds()).thenReturn(300);
        when(config.getCleanupIntervalSeconds()).thenReturn(60);
    }

    @AfterEach
    void tearDown() {
        if (relayController != null && relayController.isRunning()) {
            relayController.stop();
        }
        scheduler.shutdownNow();
    }

    @Nested
    @DisplayName("Relay Controller Lifecycle")
    class RelayControllerLifecycle {

        @Test
        @DisplayName("should not be running initially")
        void shouldNotBeRunningInitially() {
            relayController = new KailleraRelayController(threadPool, scheduler, config);

            assertFalse(relayController.isRunning());
        }

        @Test
        @DisplayName("should have empty V086 relays initially")
        void shouldHaveEmptyV086RelaysInitially() {
            relayController = new KailleraRelayController(threadPool, scheduler, config);

            assertNotNull(relayController.getV086Relays());
            assertTrue(relayController.getV086Relays().isEmpty());
        }

        @Test
        @DisplayName("should track metrics correctly")
        void shouldTrackMetricsCorrectly() {
            relayController = new KailleraRelayController(threadPool, scheduler, config);

            assertEquals(0, relayController.getTotalConnections());
            assertEquals(0, relayController.getBytesRelayed());
            assertEquals(0, relayController.getParseErrors());
        }
    }

    @Nested
    @DisplayName("Relay Configuration")
    class RelayConfiguration {

        @Test
        @DisplayName("should use configured listen port")
        void shouldUseConfiguredListenPort() {
            relayController = new KailleraRelayController(threadPool, scheduler, config);

            assertEquals(TEST_LISTEN_PORT, relayController.getListenPort());
        }

        @Test
        @DisplayName("should use configured backend address")
        void shouldUseConfiguredBackendAddress() {
            relayController = new KailleraRelayController(threadPool, scheduler, config);

            assertEquals(TEST_BACKEND_HOST,
                    relayController.getServerSocketAddress().getHostString());
            assertEquals(TEST_BACKEND_PORT, relayController.getServerSocketAddress().getPort());
        }
    }

    @Nested
    @DisplayName("V086 Relay Controller")
    class V086RelayControllerTests {

        @Test
        @DisplayName("should create V086 relay with correct port")
        void shouldCreateV086RelayWithCorrectPort() {
            int testPort = 27889;
            V086RelayController v086Relay = new V086RelayController(threadPool, testPort,
                    new java.net.InetSocketAddress(TEST_BACKEND_HOST, testPort),
                    config.getMaxConnections(), config.getBufferSize());

            assertEquals(testPort, v086Relay.getListenPort());
            assertFalse(v086Relay.isRunning());
        }

        @Test
        @DisplayName("should track message numbers")
        void shouldTrackMessageNumbers() {
            int testPort = 27890;
            V086RelayController v086Relay = new V086RelayController(threadPool, testPort,
                    new java.net.InetSocketAddress(TEST_BACKEND_HOST, testPort),
                    config.getMaxConnections(), config.getBufferSize());

            // Initial values are -1 (no messages received yet)
            assertEquals(-1, v086Relay.getLastClientMessageNumber());
            assertEquals(-1, v086Relay.getLastServerMessageNumber());
        }
    }

    @Nested
    @DisplayName("Protocol Message Helpers")
    class ProtocolMessageHelpers {

        @Test
        @DisplayName("should construct HELLO message correctly")
        void shouldConstructHelloMessage() {
            ByteBuffer buffer = ByteBuffer.allocate(256);
            String protocol = "0.83";
            int port = 27888;

            // HELLO format: "HELLO" + 0x00 + protocol + 0x00 + port(2 bytes)
            buffer.put("HELLO".getBytes(CHARSET));
            buffer.put((byte) 0x00);
            buffer.put(protocol.getBytes(CHARSET));
            buffer.put((byte) 0x00);
            buffer.putShort((short) port);
            buffer.flip();

            assertTrue(buffer.remaining() > 0);
            assertEquals('H', (char) buffer.get());
        }

        @Test
        @DisplayName("should construct HELLOD00D response correctly")
        void shouldConstructHellod00dResponse() {
            ByteBuffer buffer = ByteBuffer.allocate(256);
            int assignedPort = 27889;

            // HELLOD00D format: "HELLOD00D" + 0x00 + port(4 bytes)
            buffer.put("HELLOD00D".getBytes(CHARSET));
            buffer.put((byte) 0x00);
            buffer.putInt(assignedPort);
            buffer.flip();

            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            assertTrue(new String(data, 0, 9, CHARSET).equals("HELLOD00D"));
        }
    }

    @Nested
    @DisplayName("Idle Timeout Handling")
    class IdleTimeoutHandling {

        @Test
        @DisplayName("V086 relay should report idle status correctly")
        void v086RelayShouldReportIdleStatusCorrectly() {
            int testPort = 27891;
            V086RelayController v086Relay = new V086RelayController(threadPool, testPort,
                    new java.net.InetSocketAddress(TEST_BACKEND_HOST, testPort),
                    config.getMaxConnections(), config.getBufferSize());

            // Newly created relay (not started) has no activity, so isIdle returns false
            assertFalse(v086Relay.isIdle(1000));

            // Even with 0ms timeout, a relay with no activity timestamp returns false
            assertFalse(v086Relay.isIdle(0));
        }
    }
}
