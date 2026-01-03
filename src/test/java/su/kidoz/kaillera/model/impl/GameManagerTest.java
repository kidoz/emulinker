package su.kidoz.kaillera.model.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import su.kidoz.kaillera.model.KailleraGame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameManager Tests")
class GameManagerTest {

    private GameManager gameManager;

    @BeforeEach
    void setUp() {
        gameManager = new GameManager(100);
    }

    @Nested
    @DisplayName("Game ID Generation")
    class GameIdGeneration {
        @Test
        @DisplayName("should generate sequential IDs starting from 1")
        void shouldGenerateSequentialIds() {
            assertEquals(1, gameManager.getNextGameID());
            assertEquals(2, gameManager.getNextGameID());
            assertEquals(3, gameManager.getNextGameID());
        }

        @Test
        @DisplayName("should wrap around at 0xFFFF")
        void shouldWrapAroundAtMax() {
            // Generate IDs up to the max
            for (int i = 1; i < 0xFFFF; i++) {
                gameManager.getNextGameID();
            }
            assertEquals(0xFFFF, gameManager.getNextGameID());
            // Should wrap to 1
            assertEquals(1, gameManager.getNextGameID());
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
                            int id = gameManager.getNextGameID();
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
    @DisplayName("Game Storage")
    class GameStorage {
        @Test
        @DisplayName("should add and retrieve game")
        void shouldAddAndRetrieveGame() {
            KailleraGameImpl game = mock(KailleraGameImpl.class);
            when(game.getID()).thenReturn(1);

            gameManager.addGame(game);
            assertEquals(game, gameManager.getGame(1));
        }

        @Test
        @DisplayName("should return null for non-existent game")
        void shouldReturnNullForNonExistent() {
            assertNull(gameManager.getGame(999));
        }

        @Test
        @DisplayName("should remove game")
        void shouldRemoveGame() {
            KailleraGameImpl game = mock(KailleraGameImpl.class);
            when(game.getID()).thenReturn(1);

            gameManager.addGame(game);
            KailleraGameImpl removed = gameManager.removeGame(1);

            assertEquals(game, removed);
            assertNull(gameManager.getGame(1));
        }

        @Test
        @DisplayName("should check if game exists")
        void shouldCheckIfGameExists() {
            KailleraGameImpl game = mock(KailleraGameImpl.class);
            when(game.getID()).thenReturn(1);

            assertFalse(gameManager.containsGame(1));
            gameManager.addGame(game);
            assertTrue(gameManager.containsGame(1));
        }
    }

    @Nested
    @DisplayName("Game Collection")
    class GameCollection {
        @Test
        @DisplayName("should return all games")
        void shouldReturnAllGames() {
            KailleraGameImpl game1 = mock(KailleraGameImpl.class);
            KailleraGameImpl game2 = mock(KailleraGameImpl.class);
            when(game1.getID()).thenReturn(1);
            when(game2.getID()).thenReturn(2);

            gameManager.addGame(game1);
            gameManager.addGame(game2);

            Collection<KailleraGameImpl> games = gameManager.getGames();
            assertEquals(2, games.size());
            assertTrue(games.contains(game1));
            assertTrue(games.contains(game2));
        }

        @Test
        @DisplayName("should return correct game count")
        void shouldReturnCorrectCount() {
            assertEquals(0, gameManager.getNumGames());

            KailleraGameImpl game = mock(KailleraGameImpl.class);
            when(game.getID()).thenReturn(1);
            gameManager.addGame(game);

            assertEquals(1, gameManager.getNumGames());
        }
    }

    @Nested
    @DisplayName("Games Playing Count")
    class GamesPlayingCount {
        @Test
        @DisplayName("should count games not in waiting status")
        void shouldCountGamesPlaying() {
            KailleraGameImpl waitingGame = mock(KailleraGameImpl.class);
            KailleraGameImpl playingGame1 = mock(KailleraGameImpl.class);
            KailleraGameImpl playingGame2 = mock(KailleraGameImpl.class);

            when(waitingGame.getID()).thenReturn(1);
            when(playingGame1.getID()).thenReturn(2);
            when(playingGame2.getID()).thenReturn(3);

            when(waitingGame.getStatus()).thenReturn((int) KailleraGame.STATUS_WAITING);
            when(playingGame1.getStatus()).thenReturn((int) KailleraGame.STATUS_PLAYING);
            when(playingGame2.getStatus()).thenReturn((int) KailleraGame.STATUS_SYNCHRONIZING);

            gameManager.addGame(waitingGame);
            gameManager.addGame(playingGame1);
            gameManager.addGame(playingGame2);

            assertEquals(2, gameManager.getNumGamesPlaying());
        }

        @Test
        @DisplayName("should return zero when all games are waiting")
        void shouldReturnZeroWhenAllWaiting() {
            KailleraGameImpl game = mock(KailleraGameImpl.class);
            when(game.getID()).thenReturn(1);
            when(game.getStatus()).thenReturn((int) KailleraGame.STATUS_WAITING);

            gameManager.addGame(game);

            assertEquals(0, gameManager.getNumGamesPlaying());
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {
        @Test
        @DisplayName("should clear all games")
        void shouldClearAllGames() {
            KailleraGameImpl game = mock(KailleraGameImpl.class);
            when(game.getID()).thenReturn(1);

            gameManager.addGame(game);
            gameManager.clear();

            assertEquals(0, gameManager.getNumGames());
            assertNull(gameManager.getGame(1));
        }
    }
}
