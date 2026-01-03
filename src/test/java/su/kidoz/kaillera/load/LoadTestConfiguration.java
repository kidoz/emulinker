package su.kidoz.kaillera.load;

import java.time.Duration;

/**
 * Configuration constants for load testing.
 *
 * <p>
 * Values can be overridden via system properties:
 * <ul>
 * <li>{@code load.clients} - Number of concurrent clients (default: 100)</li>
 * <li>{@code load.timeout} - Test timeout in seconds (default: 60)</li>
 * <li>{@code load.messages} - Messages per client in chat tests (default:
 * 10)</li>
 * <li>{@code load.players} - Players per game in sync tests (default: 4)</li>
 * <li>{@code load.latency} - Injected latency in ms (default: 50)</li>
 * <li>{@code load.successRate} - Minimum success rate % (default: 95.0)</li>
 * </ul>
 *
 * <p>
 * Example: {@code ./gradlew test -Dload.tests=true -Dload.clients=50}
 */
public final class LoadTestConfiguration {

    private LoadTestConfiguration() {
    }

    /** Number of concurrent clients for stress tests. */
    public static final int CONCURRENT_CLIENTS = getInt("load.clients", 100);

    /** Timeout for individual client operations. */
    public static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(5);

    /** Timeout for full load test completion. */
    public static final Duration TEST_TIMEOUT = Duration.ofSeconds(getLong("load.timeout", 60));

    /** Number of messages per client in chat flood tests. */
    public static final int MESSAGES_PER_CLIENT = getInt("load.messages", 10);

    /** Delay between operations in milliseconds. */
    public static final long OPERATION_DELAY_MS = 10;

    /** Number of game data frames per client in throughput tests. */
    public static final int FRAMES_PER_CLIENT = 100;

    /** Number of players per game in game sync tests. */
    public static final int PLAYERS_PER_GAME = getInt("load.players", 4);

    /** Simulated latency for latency injection tests (ms). */
    public static final long INJECTED_LATENCY_MS = getLong("load.latency", 50);

    /** Minimum success rate required for tests to pass (percentage). */
    public static final double MIN_SUCCESS_RATE = getDouble("load.successRate", 95.0);

    /** Concurrency levels for parameterized tests. */
    public static final int[] CONCURRENCY_LEVELS = {10, 50, 100};

    private static int getInt(String property, int defaultValue) {
        String value = System.getProperty(property);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private static long getLong(String property, long defaultValue) {
        String value = System.getProperty(property);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private static double getDouble(String property, double defaultValue) {
        String value = System.getProperty(property);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
