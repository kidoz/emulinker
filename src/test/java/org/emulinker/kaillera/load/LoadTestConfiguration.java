package org.emulinker.kaillera.load;

import java.time.Duration;

/**
 * Configuration constants for load testing.
 */
public final class LoadTestConfiguration {

    private LoadTestConfiguration() {
    }

    /** Number of concurrent clients for stress tests. */
    public static final int CONCURRENT_CLIENTS = 100;

    /** Timeout for individual client operations. */
    public static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(5);

    /** Timeout for full load test completion. */
    public static final Duration TEST_TIMEOUT = Duration.ofSeconds(60);

    /** Number of messages per client in chat flood tests. */
    public static final int MESSAGES_PER_CLIENT = 10;

    /** Delay between operations in milliseconds. */
    public static final long OPERATION_DELAY_MS = 10;

    /** Number of game data frames per client in throughput tests. */
    public static final int FRAMES_PER_CLIENT = 100;
}
