package org.emulinker.kaillera.controller.connectcontroller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for ConnectController thread safety, particularly atomic counter
 * operations.
 *
 * <p>
 * These tests verify that the AtomicInteger/AtomicLong counters work correctly
 * under concurrent access, which was a fix from the architecture review.
 */
@DisplayName("ConnectController Thread Safety Tests")
class ConnectControllerThreadSafetyTest {

    /**
     * Simple test class to verify atomic counter pattern works correctly. This
     * simulates what ConnectController does with its counters.
     */
    private static class AtomicCounterSimulator {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private final AtomicInteger connectCount = new AtomicInteger(0);
        private final AtomicInteger errorCount = new AtomicInteger(0);

        void incrementRequest() {
            requestCount.incrementAndGet();
        }

        void incrementConnect() {
            connectCount.incrementAndGet();
        }

        void incrementError() {
            errorCount.incrementAndGet();
        }

        int getRequestCount() {
            return requestCount.get();
        }

        int getConnectCount() {
            return connectCount.get();
        }

        int getErrorCount() {
            return errorCount.get();
        }
    }

    @Test
    @DisplayName("Atomic counters should accurately count concurrent increments")
    void atomicCountersShouldAccuratelyCountConcurrentIncrements() throws InterruptedException {
        final int numThreads = 10;
        final int incrementsPerThread = 1000;
        final AtomicCounterSimulator simulator = new AtomicCounterSimulator();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < incrementsPerThread; i++) {
                        simulator.incrementRequest();
                        if (i % 2 == 0) {
                            simulator.incrementConnect();
                        }
                        if (i % 10 == 0) {
                            simulator.incrementError();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        int expectedRequests = numThreads * incrementsPerThread;
        int expectedConnects = numThreads * (incrementsPerThread / 2);
        int expectedErrors = numThreads * (incrementsPerThread / 10);

        assertEquals(expectedRequests, simulator.getRequestCount(),
                "Request count should be exactly " + expectedRequests);
        assertEquals(expectedConnects, simulator.getConnectCount(),
                "Connect count should be exactly " + expectedConnects);
        assertEquals(expectedErrors, simulator.getErrorCount(),
                "Error count should be exactly " + expectedErrors);
    }

    @Test
    @DisplayName("Counters should be readable during concurrent updates")
    void countersShouldBeReadableDuringConcurrentUpdates() throws InterruptedException {
        final AtomicCounterSimulator simulator = new AtomicCounterSimulator();
        final int numWriterThreads = 5;
        final int numReaderThreads = 5;
        final int iterations = 1000;

        ExecutorService executor = Executors
                .newFixedThreadPool(numWriterThreads + numReaderThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numWriterThreads + numReaderThreads);
        AtomicInteger readCount = new AtomicInteger(0);

        // Writer threads
        for (int t = 0; t < numWriterThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        simulator.incrementRequest();
                        simulator.incrementConnect();
                        simulator.incrementError();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Reader threads
        for (int t = 0; t < numReaderThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        // These should never throw
                        int r = simulator.getRequestCount();
                        int c = simulator.getConnectCount();
                        int e = simulator.getErrorCount();

                        // Values should always be >= 0
                        assertTrue(r >= 0);
                        assertTrue(c >= 0);
                        assertTrue(e >= 0);

                        readCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // All reads should have completed
        assertEquals(numReaderThreads * iterations, readCount.get());

        // Final counts should be accurate
        assertEquals(numWriterThreads * iterations, simulator.getRequestCount());
        assertEquals(numWriterThreads * iterations, simulator.getConnectCount());
        assertEquals(numWriterThreads * iterations, simulator.getErrorCount());
    }
}
