package su.kidoz.kaillera.controller.v086;

import java.util.OptionalInt;

/**
 * Manages allocation and release of dynamic ports for client connections.
 *
 * <p>
 * Ports are allocated from a pre-configured range and returned to the pool when
 * client connections are closed. This interface allows for thread-safe port
 * management without exposing the underlying data structure.
 */
public interface PortAllocator {

    /**
     * Allocates an available port from the pool.
     *
     * @return the allocated port number, or empty if no ports are available
     */
    OptionalInt allocate();

    /**
     * Releases a port back to the pool for reuse.
     *
     * @param port
     *            the port to release
     */
    void release(int port);

    /**
     * Returns the number of available ports in the pool.
     *
     * @return count of available ports
     */
    int availableCount();
}
