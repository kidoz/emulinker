package org.emulinker.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExecutorService implementation using Java 25 virtual threads (Project Loom).
 *
 * <p>
 * Virtual threads enable high concurrency with low resource usage - each task
 * gets its own lightweight virtual thread that is scheduled by the JVM rather
 * than the OS.
 *
 * <p>
 * This class provides compatibility methods (getActiveCount, getPoolSize) for
 * existing debug logging that previously relied on ThreadPoolExecutor.
 */
public class EmuLinkerExecutor implements ExecutorService {
    private static final Logger log = LoggerFactory.getLogger(EmuLinkerExecutor.class);

    private final ExecutorService delegate;
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final AtomicInteger totalSubmitted = new AtomicInteger(0);

    public EmuLinkerExecutor() {
        this.delegate = Executors.newVirtualThreadPerTaskExecutor();
        log.info("Kaillux executor initialized with virtual threads (Project Loom)");
    }

    /**
     * Returns the approximate number of threads that are actively executing tasks.
     * Provided for compatibility with existing debug logging.
     */
    public int getActiveCount() {
        return activeCount.get();
    }

    /**
     * Returns the total number of tasks that have been submitted. Provided for
     * compatibility with existing debug logging.
     */
    public int getPoolSize() {
        return totalSubmitted.get();
    }

    /**
     * Returns Integer.MAX_VALUE since virtual threads have no practical limit.
     * Provided for compatibility with admin REST API.
     */
    public int getMaximumPoolSize() {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns the total number of tasks submitted (same as poolSize for virtual
     * threads). Provided for compatibility with admin REST API.
     */
    public long getTaskCount() {
        return totalSubmitted.get();
    }

    @Override
    public void execute(Runnable command) {
        totalSubmitted.incrementAndGet();
        activeCount.incrementAndGet();
        delegate.execute(() -> {
            try {
                command.run();
            } finally {
                activeCount.decrementAndGet();
            }
        });
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        totalSubmitted.incrementAndGet();
        activeCount.incrementAndGet();
        return delegate.submit(() -> {
            try {
                return task.call();
            } finally {
                activeCount.decrementAndGet();
            }
        });
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        totalSubmitted.incrementAndGet();
        activeCount.incrementAndGet();
        return delegate.submit(() -> {
            try {
                task.run();
            } finally {
                activeCount.decrementAndGet();
            }
        }, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        totalSubmitted.incrementAndGet();
        activeCount.incrementAndGet();
        return delegate.submit(() -> {
            try {
                task.run();
            } finally {
                activeCount.decrementAndGet();
            }
        });
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
            TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }
}
