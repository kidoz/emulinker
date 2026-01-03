package su.kidoz.kaillera.model.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetSocketAddress;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.DefaultEventDispatcher;
import su.kidoz.kaillera.model.event.EventDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for KailleraGameImpl, particularly the getPlayer() NPE fix.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KailleraGameImpl Tests")
class KailleraGameImplTest {

    private static final int BUFFER_SIZE = 1024;
    private static final int TIMEOUT_MILLIS = 1000;
    private static final int DESYNCH_TIMEOUTS = 4;

    @Mock
    private KailleraServerImpl server;

    private EventDispatcher eventDispatcher;

    private KailleraUserImpl owner;
    private KailleraGameImpl game;

    @BeforeEach
    void setUp() {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 27888);
        eventDispatcher = new DefaultEventDispatcher();
        owner = new KailleraUserImpl(1, "v086", address, eventDispatcher, server);
        owner.setName("GameOwner");

        game = new KailleraGameImpl(1, "TestROM", owner, server, BUFFER_SIZE, TIMEOUT_MILLIS,
                DESYNCH_TIMEOUTS);
    }

    @Nested
    @DisplayName("getPlayer() NPE protection")
    class GetPlayerNPEProtection {

        @Test
        @DisplayName("should return null for playerNumber 0")
        void shouldReturnNullForPlayerNumberZero() {
            KailleraUser result = game.getPlayer(0);
            assertNull(result, "getPlayer(0) should return null");
        }

        @Test
        @DisplayName("should return null for negative playerNumber")
        void shouldReturnNullForNegativePlayerNumber() {
            KailleraUser result = game.getPlayer(-1);
            assertNull(result, "getPlayer(-1) should return null");

            result = game.getPlayer(-100);
            assertNull(result, "getPlayer(-100) should return null");
        }

        @Test
        @DisplayName("should return null for playerNumber exceeding player count")
        void shouldReturnNullForExceedingPlayerNumber() {
            // No players joined yet
            KailleraUser result = game.getPlayer(1);
            assertNull(result, "getPlayer(1) should return null when no players joined");

            result = game.getPlayer(100);
            assertNull(result, "getPlayer(100) should return null");
        }

        @Test
        @DisplayName("should not throw exception for invalid player numbers")
        void shouldNotThrowForInvalidPlayerNumbers() {
            // These should all complete without throwing
            assertDoesNotThrow(() -> game.getPlayer(-1));
            assertDoesNotThrow(() -> game.getPlayer(0));
            assertDoesNotThrow(() -> game.getPlayer(Integer.MIN_VALUE));
            assertDoesNotThrow(() -> game.getPlayer(Integer.MAX_VALUE));
        }
    }

    @Nested
    @DisplayName("Basic functionality")
    class BasicFunctionality {

        @Test
        @DisplayName("should return correct game ID")
        void shouldReturnGameId() {
            assertEquals(1, game.getID());
        }

        @Test
        @DisplayName("should return correct ROM name")
        void shouldReturnRomName() {
            assertEquals("TestROM", game.getRomName());
        }

        @Test
        @DisplayName("should return correct owner")
        void shouldReturnOwner() {
            assertEquals(owner, game.getOwner());
        }

        @Test
        @DisplayName("should start with waiting status")
        void shouldStartWithWaitingStatus() {
            assertEquals(0, game.getStatus()); // STATUS_WAITING = 0
        }

        @Test
        @DisplayName("should return 0 players initially")
        void shouldReturnZeroPlayersInitially() {
            assertEquals(0, game.getNumPlayers());
        }
    }
}
