package su.kidoz.kaillera.model.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.model.impl.KailleraUserImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserManager Tests")
class UserManagerTest {

    private UserManager userManager;

    @BeforeEach
    void setUp() {
        userManager = new UserManager(100);
    }

    @Nested
    @DisplayName("User ID Generation")
    class UserIdGeneration {
        @Test
        @DisplayName("should generate sequential IDs starting from 1")
        void shouldGenerateSequentialIds() {
            assertEquals(1, userManager.getNextUserID());
            assertEquals(2, userManager.getNextUserID());
            assertEquals(3, userManager.getNextUserID());
        }

        @Test
        @DisplayName("should wrap around at 0xFFFF")
        void shouldWrapAroundAtMax() {
            // Generate IDs up to the max
            for (int i = 1; i < 0xFFFF; i++) {
                userManager.getNextUserID();
            }
            assertEquals(0xFFFF, userManager.getNextUserID());
            // Should wrap to 1
            assertEquals(1, userManager.getNextUserID());
        }

        @Test
        @DisplayName("should be thread-safe")
        void shouldBeThreadSafe() throws InterruptedException {
            int numThreads = 10;
            int idsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger duplicates = new AtomicInteger(0);
            java.util.Set<Integer> ids = java.util.concurrent.ConcurrentHashMap.newKeySet();

            for (int t = 0; t < numThreads; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < idsPerThread; i++) {
                            int id = userManager.getNextUserID();
                            if (!ids.add(id)) {
                                duplicates.incrementAndGet();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();
            assertEquals(0, duplicates.get(), "Should have no duplicate IDs");
            assertEquals(numThreads * idsPerThread, ids.size());
        }
    }

    @Nested
    @DisplayName("User Storage")
    class UserStorage {
        @Test
        @DisplayName("should add and retrieve user")
        void shouldAddAndRetrieveUser() {
            KailleraUserImpl user = mock(KailleraUserImpl.class);
            when(user.getID()).thenReturn(1);

            userManager.addUser(user);
            assertEquals(user, userManager.getUser(1));
        }

        @Test
        @DisplayName("should return null for non-existent user")
        void shouldReturnNullForNonExistent() {
            assertNull(userManager.getUser(999));
        }

        @Test
        @DisplayName("should remove user")
        void shouldRemoveUser() {
            KailleraUserImpl user = mock(KailleraUserImpl.class);
            when(user.getID()).thenReturn(1);

            userManager.addUser(user);
            KailleraUserImpl removed = userManager.removeUser(1);

            assertEquals(user, removed);
            assertNull(userManager.getUser(1));
        }

        @Test
        @DisplayName("should check if user exists")
        void shouldCheckIfUserExists() {
            KailleraUserImpl user = mock(KailleraUserImpl.class);
            when(user.getID()).thenReturn(1);

            assertFalse(userManager.containsUser(1));
            userManager.addUser(user);
            assertTrue(userManager.containsUser(1));
        }
    }

    @Nested
    @DisplayName("User Collection")
    class UserCollection {
        @Test
        @DisplayName("should return all users")
        void shouldReturnAllUsers() {
            KailleraUserImpl user1 = mock(KailleraUserImpl.class);
            KailleraUserImpl user2 = mock(KailleraUserImpl.class);
            when(user1.getID()).thenReturn(1);
            when(user2.getID()).thenReturn(2);

            userManager.addUser(user1);
            userManager.addUser(user2);

            Collection<KailleraUserImpl> users = userManager.getUsers();
            assertEquals(2, users.size());
            assertTrue(users.contains(user1));
            assertTrue(users.contains(user2));
        }

        @Test
        @DisplayName("should return correct user count")
        void shouldReturnCorrectCount() {
            assertEquals(0, userManager.getNumUsers());

            KailleraUserImpl user = mock(KailleraUserImpl.class);
            when(user.getID()).thenReturn(1);
            userManager.addUser(user);

            assertEquals(1, userManager.getNumUsers());
        }

        @Test
        @DisplayName("should detect empty state")
        void shouldDetectEmptyState() {
            assertTrue(userManager.isEmpty());

            KailleraUserImpl user = mock(KailleraUserImpl.class);
            when(user.getID()).thenReturn(1);
            userManager.addUser(user);

            assertFalse(userManager.isEmpty());
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {
        @Test
        @DisplayName("should stop all users")
        void shouldStopAllUsers() {
            KailleraUserImpl user1 = mock(KailleraUserImpl.class);
            KailleraUserImpl user2 = mock(KailleraUserImpl.class);
            when(user1.getID()).thenReturn(1);
            when(user2.getID()).thenReturn(2);

            userManager.addUser(user1);
            userManager.addUser(user2);
            userManager.stopAllUsers();

            verify(user1).stop();
            verify(user2).stop();
            assertTrue(userManager.isEmpty());
        }

        @Test
        @DisplayName("should clear all users without stopping")
        void shouldClearAllUsers() {
            KailleraUserImpl user = mock(KailleraUserImpl.class);
            when(user.getID()).thenReturn(1);

            userManager.addUser(user);
            userManager.clear();

            verify(user, never()).stop();
            assertTrue(userManager.isEmpty());
        }
    }
}
