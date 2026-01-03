package su.kidoz.kaillera.controller.v086;

import java.util.OptionalInt;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe implementation of {@link PortAllocator} using a concurrent queue.
 *
 * <p>
 * Ports are initialized from a contiguous range and managed in FIFO order.
 */
public final class PortAllocatorImpl implements PortAllocator {

    private final Queue<Integer> portQueue;

    /**
     * Creates a port allocator for the specified range.
     *
     * @param startPort
     *            the first port in the range (inclusive)
     * @param count
     *            the number of ports to allocate
     */
    public PortAllocatorImpl(int startPort, int count) {
        this.portQueue = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < count; i++) {
            portQueue.add(startPort + i);
        }
    }

    @Override
    public OptionalInt allocate() {
        Integer port = portQueue.poll();
        return port != null ? OptionalInt.of(port) : OptionalInt.empty();
    }

    @Override
    public void release(int port) {
        portQueue.add(port);
    }

    @Override
    public int availableCount() {
        return portQueue.size();
    }
}
