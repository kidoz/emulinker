package su.kidoz.kaillera.load;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collects and reports metrics for load testing.
 *
 * <p>
 * Tracks timing, success/failure counts, and error classification.
 */
public class LoadTestMetrics {

    /** Error types for classification. */
    public enum ErrorType {
        CONNECTION_TIMEOUT, LOGIN_TIMEOUT, PROTOCOL_ERROR, SERVER_FULL, STATE_VIOLATION, NETWORK_ERROR, UNKNOWN
    }

    /** Operation types for timing. */
    public enum Operation {
        CONNECT, LOGIN, CHAT, CREATE_GAME, JOIN_GAME, START_GAME, GAME_DATA, QUIT
    }

    private final ConcurrentLinkedQueue<Long> connectTimes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> loginTimes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> chatTimes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> gameCreateTimes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> gameJoinTimes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> gameStartTimes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> gameDataTimes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> quitTimes = new ConcurrentLinkedQueue<>();

    private final LongAdder totalOperations = new LongAdder();
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();

    private final Map<ErrorType, AtomicInteger> errorCounts = new EnumMap<>(ErrorType.class);

    private final long startTimeNanos;

    public LoadTestMetrics() {
        this.startTimeNanos = System.nanoTime();
        for (ErrorType type : ErrorType.values()) {
            errorCounts.put(type, new AtomicInteger(0));
        }
    }

    /** Records a successful operation with timing. */
    public void recordSuccess(Operation operation, long durationMs) {
        totalOperations.increment();
        successCount.increment();
        getTimingQueue(operation).add(durationMs);
    }

    /** Records a failed operation with error classification. */
    public void recordFailure(Operation operation, ErrorType errorType) {
        totalOperations.increment();
        failureCount.increment();
        errorCounts.get(errorType).incrementAndGet();
    }

    /** Records a failure from an exception. */
    public void recordFailure(Operation operation, Exception e) {
        recordFailure(operation, classifyError(e));
    }

    private ConcurrentLinkedQueue<Long> getTimingQueue(Operation operation) {
        return switch (operation) {
            case CONNECT -> connectTimes;
            case LOGIN -> loginTimes;
            case CHAT -> chatTimes;
            case CREATE_GAME -> gameCreateTimes;
            case JOIN_GAME -> gameJoinTimes;
            case START_GAME -> gameStartTimes;
            case GAME_DATA -> gameDataTimes;
            case QUIT -> quitTimes;
        };
    }

    /** Classifies an exception into an error type. */
    public static ErrorType classifyError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (message.contains("timeout")) {
            if (message.contains("login") || message.contains("ack")) {
                return ErrorType.LOGIN_TIMEOUT;
            }
            return ErrorType.CONNECTION_TIMEOUT;
        }
        if (message.contains("full") || message.contains("max")) {
            return ErrorType.SERVER_FULL;
        }
        if (message.contains("state") || message.contains("not logged in")) {
            return ErrorType.STATE_VIOLATION;
        }
        if (message.contains("protocol") || message.contains("malformed")) {
            return ErrorType.PROTOCOL_ERROR;
        }
        if (e instanceof java.io.IOException) {
            return ErrorType.NETWORK_ERROR;
        }
        return ErrorType.UNKNOWN;
    }

    /** Calculates percentile from a collection of values. */
    public static long percentile(long[] sortedValues, double percentile) {
        if (sortedValues.length == 0) {
            return 0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.length) - 1;
        return sortedValues[Math.max(0, Math.min(index, sortedValues.length - 1))];
    }

    /** Gets statistics for an operation. */
    public OperationStats getStats(Operation operation) {
        ConcurrentLinkedQueue<Long> times = getTimingQueue(operation);
        long[] values = times.stream().mapToLong(Long::longValue).sorted().toArray();

        if (values.length == 0) {
            return new OperationStats(0, 0, 0, 0, 0, 0);
        }

        return new OperationStats(values.length, Arrays.stream(values).min().orElse(0),
                Arrays.stream(values).max().orElse(0), percentile(values, 50),
                percentile(values, 95), percentile(values, 99));
    }

    /** Gets the total elapsed time in milliseconds. */
    public long getElapsedTimeMs() {
        return (System.nanoTime() - startTimeNanos) / 1_000_000;
    }

    /** Gets the success rate as a percentage. */
    public double getSuccessRate() {
        long total = totalOperations.sum();
        if (total == 0) {
            return 100.0;
        }
        return (successCount.sum() * 100.0) / total;
    }

    /** Generates a summary report. */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Load Test Results ===\n");
        sb.append(String.format("Duration: %d ms%n", getElapsedTimeMs()));
        sb.append(String.format("Total Operations: %d%n", totalOperations.sum()));
        sb.append(String.format("Success: %d (%.1f%%)%n", successCount.sum(), getSuccessRate()));
        sb.append(String.format("Failures: %d%n", failureCount.sum()));

        sb.append("\n--- Timing (ms) ---\n");
        sb.append(String.format("%-12s %6s %6s %6s %6s %6s %6s%n", "Operation", "Count", "Min",
                "Max", "p50", "p95", "p99"));

        for (Operation op : Operation.values()) {
            OperationStats stats = getStats(op);
            if (stats.count() > 0) {
                sb.append(String.format("%-12s %6d %6d %6d %6d %6d %6d%n", op.name(), stats.count(),
                        stats.min(), stats.max(), stats.p50(), stats.p95(), stats.p99()));
            }
        }

        sb.append("\n--- Errors ---\n");
        for (ErrorType type : ErrorType.values()) {
            int count = errorCounts.get(type).get();
            if (count > 0) {
                sb.append(String.format("%-20s: %d%n", type.name(), count));
            }
        }

        return sb.toString();
    }

    /** Statistics for a single operation type. */
    public record OperationStats(int count, long min, long max, long p50, long p95, long p99) {
    }
}
