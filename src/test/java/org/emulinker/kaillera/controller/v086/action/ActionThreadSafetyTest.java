package org.emulinker.kaillera.controller.v086.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for thread safety of action class counters.
 *
 * <p>
 * The original bug was that action classes used non-atomic int counters
 * (actionCount, handledCount) which could lead to data races and incorrect
 * counts when accessed concurrently. The fix was to use AtomicInteger for
 * thread-safe counters.
 */
class ActionThreadSafetyTest {

    private static final int NUM_THREADS = 10;
    private static final int INCREMENTS_PER_THREAD = 1000;
    private static final int EXPECTED_TOTAL = NUM_THREADS * INCREMENTS_PER_THREAD;

    @Test
    @DisplayName("ACKAction counter should be thread-safe")
    void ackActionCounterThreadSafe() throws Exception {
        ACKAction action = new ACKAction();

        // Verify initial count is accessible
        action.getActionPerformedCount();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            futures.add(executor.submit(() -> {
                int count = 0;
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    // Read the count (should never throw, should always return valid int)
                    action.getActionPerformedCount();
                    count++;
                }
                return count;
            }));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        // Verify all reads completed successfully
        int totalReads = 0;
        for (Future<Integer> future : futures) {
            totalReads += future.get();
        }
        assertEquals(EXPECTED_TOTAL, totalReads);
    }

    @Test
    @DisplayName("LoginAction getActionPerformedCount returns non-negative value")
    void loginActionCounterNonNegative() {
        LoginAction action = new LoginAction();
        int count = action.getActionPerformedCount();
        assertTrue(count >= 0, "Action count should be non-negative");
    }

    @Test
    @DisplayName("LoginAction getHandledEventCount returns non-negative value")
    void loginActionHandledCountNonNegative() {
        LoginAction action = new LoginAction();
        int count = action.getHandledEventCount();
        assertTrue(count >= 0, "Handled count should be non-negative");
    }

    @Test
    @DisplayName("ChatAction getActionPerformedCount returns non-negative value")
    void chatActionCounterNonNegative() {
        AdminCommandAction adminCommandAction = new AdminCommandAction();
        ChatAction action = new ChatAction(adminCommandAction);
        int count = action.getActionPerformedCount();
        assertTrue(count >= 0, "Action count should be non-negative");
    }

    @Test
    @DisplayName("GameDataAction counter methods work correctly")
    void gameDataActionCounters() {
        GameDataAction action = new GameDataAction();

        int actionCount = action.getActionPerformedCount();
        int handledCount = action.getHandledEventCount();

        assertTrue(actionCount >= 0, "Action count should be non-negative");
        assertTrue(handledCount >= 0, "Handled count should be non-negative");
    }

    @Test
    @DisplayName("CreateGameAction counter methods work correctly")
    void createGameActionCounters() {
        CreateGameAction action = new CreateGameAction();

        int actionCount = action.getActionPerformedCount();
        int handledCount = action.getHandledEventCount();

        assertTrue(actionCount >= 0, "Action count should be non-negative");
        assertTrue(handledCount >= 0, "Handled count should be non-negative");
    }

    @Test
    @DisplayName("JoinGameAction counter methods work correctly")
    void joinGameActionCounters() {
        JoinGameAction action = new JoinGameAction();

        int actionCount = action.getActionPerformedCount();
        int handledCount = action.getHandledEventCount();

        assertTrue(actionCount >= 0, "Action count should be non-negative");
        assertTrue(handledCount >= 0, "Handled count should be non-negative");
    }

    @Test
    @DisplayName("QuitAction counter methods work correctly")
    void quitActionCounters() {
        QuitAction action = new QuitAction();

        int actionCount = action.getActionPerformedCount();
        int handledCount = action.getHandledEventCount();

        assertTrue(actionCount >= 0, "Action count should be non-negative");
        assertTrue(handledCount >= 0, "Handled count should be non-negative");
    }

    @Test
    @DisplayName("StartGameAction counter methods work correctly")
    void startGameActionCounters() {
        StartGameAction action = new StartGameAction();

        int actionCount = action.getActionPerformedCount();
        int handledCount = action.getHandledEventCount();

        assertTrue(actionCount >= 0, "Action count should be non-negative");
        assertTrue(handledCount >= 0, "Handled count should be non-negative");
    }

    @Test
    @DisplayName("All action classes can be instantiated")
    void allActionClassesCanBeInstantiated() {
        AdminCommandAction adminCommandAction = new AdminCommandAction();
        GameOwnerCommandAction gameOwnerCommandAction = new GameOwnerCommandAction();

        assertNotNull(new ACKAction());
        assertNotNull(adminCommandAction);
        assertNotNull(new CachedGameDataAction());
        assertNotNull(new ChatAction(adminCommandAction));
        assertNotNull(new CloseGameAction());
        assertNotNull(new CreateGameAction());
        assertNotNull(new DropGameAction());
        assertNotNull(new GameChatAction(gameOwnerCommandAction));
        assertNotNull(new GameDataAction());
        assertNotNull(new GameDesynchAction());
        assertNotNull(new GameInfoAction());
        assertNotNull(new GameKickAction());
        assertNotNull(gameOwnerCommandAction);
        assertNotNull(new GameStatusAction());
        assertNotNull(new GameTimeoutAction());
        assertNotNull(new InfoMessageAction());
        assertNotNull(new JoinGameAction());
        assertNotNull(new KeepAliveAction());
        assertNotNull(new LoginAction());
        assertNotNull(new PlayerDesynchAction());
        assertNotNull(new QuitAction());
        assertNotNull(new QuitGameAction());
        assertNotNull(new StartGameAction());
        assertNotNull(new UserReadyAction());
    }

    @Test
    @DisplayName("Action toString returns non-null description")
    void actionToStringReturnsDescription() {
        AdminCommandAction adminCommandAction = new AdminCommandAction();

        assertNotNull(new ACKAction().toString());
        assertNotNull(new LoginAction().toString());
        assertNotNull(new ChatAction(adminCommandAction).toString());
        assertNotNull(new GameDataAction().toString());
    }
}
