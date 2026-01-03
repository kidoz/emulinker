package su.kidoz.kaillera.model.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.InetSocketAddress;
import su.kidoz.kaillera.model.event.KailleraEvent;
import su.kidoz.kaillera.model.event.KailleraEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for KailleraUserImpl, including: - setLoggedIn bug fix - Field
 * visibility with volatile - Event queue bounding
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KailleraUserImpl Tests")
class KailleraUserImplTest {

    @Mock
    private KailleraServerImpl server;

    @Mock
    private KailleraEventListener listener;

    private KailleraUserImpl user;

    @BeforeEach
    void setUp() {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 27888);
        user = new KailleraUserImpl(1, "v086", address, listener, server);
    }

    @Test
    @DisplayName("setLoggedIn(true) should set loggedIn to true")
    void setLoggedInTrue() {
        assertFalse(user.isLoggedIn(), "User should not be logged in initially");

        user.setLoggedIn(true);

        assertTrue(user.isLoggedIn(), "User should be logged in after setLoggedIn(true)");
    }

    @Test
    @DisplayName("setLoggedIn(false) should set loggedIn to false")
    void setLoggedInFalse() {
        // First set to true
        user.setLoggedIn(true);
        assertTrue(user.isLoggedIn());

        // Then set to false
        user.setLoggedIn(false);

        assertFalse(user.isLoggedIn(), "User should not be logged in after setLoggedIn(false)");
    }

    @Test
    @DisplayName("setLoggedIn() should set loggedIn to true")
    void setLoggedInNoArg() {
        assertFalse(user.isLoggedIn(), "User should not be logged in initially");

        user.setLoggedIn();

        assertTrue(user.isLoggedIn(), "User should be logged in after setLoggedIn()");
    }

    @Test
    @DisplayName("setLoggedIn should preserve state correctly across multiple calls")
    void setLoggedInMultipleCalls() {
        // Start false
        assertFalse(user.isLoggedIn());

        // Set true
        user.setLoggedIn(true);
        assertTrue(user.isLoggedIn());

        // Set true again (should stay true)
        user.setLoggedIn(true);
        assertTrue(user.isLoggedIn());

        // Set false
        user.setLoggedIn(false);
        assertFalse(user.isLoggedIn());

        // Set false again (should stay false)
        user.setLoggedIn(false);
        assertFalse(user.isLoggedIn());

        // Set true again
        user.setLoggedIn(true);
        assertTrue(user.isLoggedIn());
    }

    @Test
    @DisplayName("User should store and return name correctly")
    void setAndGetName() {
        user.setName("TestPlayer");
        assertEquals("TestPlayer", user.getName());
    }

    @Test
    @DisplayName("User should store and return connection type")
    void setAndGetConnectionType() {
        user.setConnectionType((byte) 3);
        assertEquals((byte) 3, user.getConnectionType());
    }

    @Test
    @DisplayName("User should store and return ping")
    void setAndGetPing() {
        user.setPing(100);
        assertEquals(100, user.getPing());
    }

    @Test
    @DisplayName("User ID should be set correctly")
    void getUserId() {
        assertEquals(1, user.getID());
    }

    @Nested
    @DisplayName("Event Queue Bounding")
    class EventQueueBounding {

        @Test
        @DisplayName("addEvent should not throw when event is null")
        void addEventShouldHandleNull() {
            // Should log error but not throw
            assertDoesNotThrow(() -> user.addEvent(null));
        }

        @Test
        @DisplayName("addEvent should accept valid events")
        void addEventShouldAcceptValidEvents() {
            KailleraEvent event = mock(KailleraEvent.class);
            assertDoesNotThrow(() -> user.addEvent(event));
        }

        @Test
        @DisplayName("addEvent should gracefully handle queue overflow")
        void addEventShouldHandleQueueOverflow() {
            // The queue is bounded to 1000 events
            // Adding more than 1000 should not throw, just log warnings
            KailleraEvent event = mock(KailleraEvent.class);

            // Add more events than the queue can hold
            for (int i = 0; i < 1100; i++) {
                assertDoesNotThrow(() -> user.addEvent(event),
                        "addEvent should not throw even when queue is full");
            }
        }
    }

    @Nested
    @DisplayName("Thread Safety - Volatile Fields")
    class VolatileFields {

        @Test
        @DisplayName("status updates should be visible across threads")
        void statusUpdatesShouldBeVisibleAcrossThreads() throws InterruptedException {
            // Simulate cross-thread visibility using volatile
            Thread writer = new Thread(() -> {
                user.setLoggedIn(true);
                user.setPing(100);
                user.setConnectionType((byte) 3);
            });

            writer.start();
            writer.join();

            // These reads should see the updated values due to volatile
            assertTrue(user.isLoggedIn());
            assertEquals(100, user.getPing());
            assertEquals((byte) 3, user.getConnectionType());
        }
    }
}
