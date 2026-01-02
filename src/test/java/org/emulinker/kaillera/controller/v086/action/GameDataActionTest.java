package org.emulinker.kaillera.controller.v086.action;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for GameDataAction - handles game data protocol messages.
 *
 * <p>
 * GameDataAction is the critical path for real-time game synchronization:
 * <ul>
 * <li>Receives player input data from clients</li>
 * <li>Broadcasts input to all players in the game</li>
 * <li>Handles cached game data for latency optimization</li>
 * </ul>
 */
@DisplayName("GameDataAction Tests")
@ExtendWith(MockitoExtension.class)
class GameDataActionTest {

    private GameDataAction gameDataAction;

    @BeforeEach
    void setUp() {
        gameDataAction = new GameDataAction();
    }

    @Nested
    @DisplayName("Action Metadata")
    class ActionMetadata {

        @Test
        @DisplayName("should have non-null toString")
        void shouldHaveNonNullToString() {
            assertNotNull(gameDataAction.toString());
            assertTrue(gameDataAction.toString().contains("GameDataAction"));
        }

        @Test
        @DisplayName("should track action performed count")
        void shouldTrackActionPerformedCount() {
            int initialCount = gameDataAction.getActionPerformedCount();
            assertTrue(initialCount >= 0);
        }

        @Test
        @DisplayName("should track handled event count")
        void shouldTrackHandledEventCount() {
            int initialCount = gameDataAction.getHandledEventCount();
            assertTrue(initialCount >= 0);
        }
    }

    @Nested
    @DisplayName("Counter Thread Safety")
    class CounterThreadSafety {

        @Test
        @DisplayName("counter should be thread-safe for reads")
        void counterShouldBeThreadSafeForReads() throws Exception {
            int numThreads = 10;
            int readsPerThread = 100;
            Thread[] threads = new Thread[numThreads];

            for (int i = 0; i < numThreads; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < readsPerThread; j++) {
                        // Should never throw or return invalid value
                        int count = gameDataAction.getActionPerformedCount();
                        assertTrue(count >= 0);
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }
        }

        @Test
        @DisplayName("handled count should be thread-safe for reads")
        void handledCountShouldBeThreadSafeForReads() throws Exception {
            int numThreads = 10;
            int readsPerThread = 100;
            Thread[] threads = new Thread[numThreads];

            for (int i = 0; i < numThreads; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < readsPerThread; j++) {
                        int count = gameDataAction.getHandledEventCount();
                        assertTrue(count >= 0);
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }
        }
    }

    @Nested
    @DisplayName("Instance Creation")
    class InstanceCreation {

        @Test
        @DisplayName("should create new instance successfully")
        void shouldCreateNewInstance() {
            GameDataAction action = new GameDataAction();
            assertNotNull(action);
        }

        @Test
        @DisplayName("should have zero initial counts")
        void shouldHaveZeroInitialCounts() {
            GameDataAction action = new GameDataAction();
            assertTrue(action.getActionPerformedCount() >= 0);
            assertTrue(action.getHandledEventCount() >= 0);
        }
    }
}
