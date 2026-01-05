package su.kidoz.kaillera.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.model.impl.GameManager;

/**
 * Collects game-level metrics for Prometheus/Micrometer integration.
 *
 * <p>
 * Tracks the following metrics:
 * <ul>
 * <li>{@code kaillera.games.created} - Counter of games created</li>
 * <li>{@code kaillera.games.started} - Counter of games that reached playing
 * state</li>
 * <li>{@code kaillera.games.completed} - Counter of games that ended
 * normally</li>
 * <li>{@code kaillera.games.active} - Gauge of currently active games</li>
 * <li>{@code kaillera.games.duration} - Timer for game play duration</li>
 * <li>{@code kaillera.games.players.synced} - Counter of "all ready" sync
 * events</li>
 * <li>{@code kaillera.games.players.desynced} - Counter of desynch
 * timeouts</li>
 * <li>{@code kaillera.games.players.dropped} - Counter of player drop
 * events</li>
 * </ul>
 */
@Component
public class GameMetricsCollector {

    private static final String METRIC_PREFIX = "kaillera.games";

    private final Counter gamesCreatedCounter;
    private final Counter gamesStartedCounter;
    private final Counter gamesCompletedCounter;
    private final Counter playersSyncedCounter;
    private final Counter playersDesyncedCounter;
    private final Counter playersDroppedCounter;
    private final Timer gameDurationTimer;

    // Track game start times for duration calculation
    private final Map<Integer, Instant> gameStartTimes = new ConcurrentHashMap<>();

    public GameMetricsCollector(MeterRegistry meterRegistry, GameManager gameManager) {
        // Counters for game lifecycle
        this.gamesCreatedCounter = Counter.builder(METRIC_PREFIX + ".created")
                .description("Total number of games created").register(meterRegistry);

        this.gamesStartedCounter = Counter.builder(METRIC_PREFIX + ".started")
                .description("Total number of games that reached playing state")
                .register(meterRegistry);

        this.gamesCompletedCounter = Counter.builder(METRIC_PREFIX + ".completed")
                .description("Total number of games that ended").register(meterRegistry);

        // Player sync counters
        this.playersSyncedCounter = Counter.builder(METRIC_PREFIX + ".players.synced")
                .description("Total number of 'all players ready' sync events")
                .register(meterRegistry);

        this.playersDesyncedCounter = Counter.builder(METRIC_PREFIX + ".players.desynced")
                .description("Total number of player desynch events").register(meterRegistry);

        this.playersDroppedCounter = Counter.builder(METRIC_PREFIX + ".players.dropped")
                .description("Total number of player drop events").register(meterRegistry);

        // Timer for game duration
        this.gameDurationTimer = Timer.builder(METRIC_PREFIX + ".duration")
                .description("Duration of games from start to completion").register(meterRegistry);

        // Gauge for active games (uses GameManager as source of truth)
        Gauge.builder(METRIC_PREFIX + ".active", gameManager, GameManager::getNumGames)
                .description("Number of currently active games").register(meterRegistry);

        // Gauge for games in playing state
        Gauge.builder(METRIC_PREFIX + ".playing", gameManager, GameManager::getNumGamesPlaying)
                .description("Number of games currently in playing state").register(meterRegistry);
    }

    /**
     * Record that a new game was created.
     */
    public void recordGameCreated() {
        gamesCreatedCounter.increment();
    }

    /**
     * Record that a game started playing.
     *
     * @param gameId
     *            the game ID for duration tracking
     */
    public void recordGameStarted(int gameId) {
        gamesStartedCounter.increment();
        gameStartTimes.put(gameId, Instant.now());
    }

    /**
     * Record that a game was completed/closed.
     *
     * @param gameId
     *            the game ID for duration calculation
     */
    public void recordGameCompleted(int gameId) {
        gamesCompletedCounter.increment();

        // Calculate and record duration if start time was tracked
        Instant startTime = gameStartTimes.remove(gameId);
        if (startTime != null) {
            Duration duration = Duration.between(startTime, Instant.now());
            gameDurationTimer.record(duration);
        }
    }

    /**
     * Record that all players in a game became synced/ready.
     */
    public void recordPlayersSynced() {
        playersSyncedCounter.increment();
    }

    /**
     * Record a player desynch event (timeout or dropped packet).
     */
    public void recordPlayerDesynced() {
        playersDesyncedCounter.increment();
    }

    /**
     * Record a player drop event.
     */
    public void recordPlayerDropped() {
        playersDroppedCounter.increment();
    }
}
