package su.kidoz.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import su.kidoz.util.EmuUtil;

/**
 * Abstract UDP packet relay that acts as a transparent proxy between clients
 * and a backend server.
 *
 * <p>
 * This class implements the {@link SmartLifecycle} interface for Spring-managed
 * lifecycle control. Subclasses must implement {@link #processClientToServer}
 * and {@link #processServerToClient} to handle protocol-specific message
 * processing.
 *
 * <p>
 * The relay uses NIO-based {@link DatagramChannel} for high-performance UDP
 * packet handling and manages per-client handler threads for bidirectional
 * traffic relay.
 */
public abstract class UDPRelay implements SmartLifecycle, Runnable {

    protected static final Logger log = LoggerFactory.getLogger(UDPRelay.class);

    private static final int DEFAULT_MAX_CONNECTIONS = 100;
    private static final int DEFAULT_BUFFER_SIZE = 2048;

    private final ExecutorService threadPool;
    private final int listenPort;
    private final InetSocketAddress serverSocketAddress;
    private final int maxConnections;
    private final int bufferSize;

    private DatagramChannel listenChannel;
    private volatile boolean running = false;
    private volatile boolean stopFlag = false;

    private final Map<InetSocketAddress, ClientHandler> clients = Collections
            .synchronizedMap(new HashMap<>());

    // Metrics
    private final AtomicLong startTime = new AtomicLong(0);
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicLong bytesRelayed = new AtomicLong(0);
    private final AtomicInteger parseErrors = new AtomicInteger(0);

    /**
     * Creates a new UDP relay.
     *
     * @param threadPool
     *            the executor service for handling client connections
     * @param listenPort
     *            the port to listen on for incoming connections
     * @param serverSocketAddress
     *            the backend server address to relay traffic to
     */
    public UDPRelay(ExecutorService threadPool, int listenPort,
            InetSocketAddress serverSocketAddress) {
        this(threadPool, listenPort, serverSocketAddress, DEFAULT_MAX_CONNECTIONS,
                DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a new UDP relay with custom settings.
     *
     * @param threadPool
     *            the executor service for handling client connections
     * @param listenPort
     *            the port to listen on for incoming connections
     * @param serverSocketAddress
     *            the backend server address to relay traffic to
     * @param maxConnections
     *            maximum concurrent connections
     * @param bufferSize
     *            buffer size for UDP packets
     */
    public UDPRelay(ExecutorService threadPool, int listenPort,
            InetSocketAddress serverSocketAddress, int maxConnections, int bufferSize) {
        this.threadPool = threadPool;
        this.listenPort = listenPort;
        this.serverSocketAddress = serverSocketAddress;
        this.maxConnections = maxConnections;
        this.bufferSize = bufferSize;
    }

    /**
     * Returns the port this relay is listening on.
     */
    public int getListenPort() {
        return listenPort;
    }

    /**
     * Returns the backend server address.
     */
    public InetSocketAddress getServerSocketAddress() {
        return serverSocketAddress;
    }

    /**
     * Returns the listen channel (for subclasses that need direct access).
     */
    protected DatagramChannel getListenChannel() {
        return listenChannel;
    }

    /**
     * Returns the current number of active client connections.
     */
    public int getActiveConnections() {
        return clients.size();
    }

    /**
     * Returns all active client handlers.
     */
    public Collection<ClientHandler> getClientHandlers() {
        return Collections.unmodifiableCollection(clients.values());
    }

    /**
     * Returns the total number of connections handled since startup.
     */
    public int getTotalConnections() {
        return totalConnections.get();
    }

    /**
     * Returns the total bytes relayed since startup.
     */
    public long getBytesRelayed() {
        return bytesRelayed.get();
    }

    /**
     * Returns the number of parse errors encountered.
     */
    public int getParseErrors() {
        return parseErrors.get();
    }

    /**
     * Returns the relay start time in milliseconds since epoch.
     */
    public long getStartTime() {
        return startTime.get();
    }

    /**
     * Increments the parse error counter.
     */
    protected void incrementParseErrors() {
        parseErrors.incrementAndGet();
    }

    /**
     * Processes a packet from client to server. Subclasses can inspect, modify, or
     * reject packets.
     *
     * @param receiveBuffer
     *            the received packet data
     * @param fromAddress
     *            the client's address
     * @param toAddress
     *            the server's address
     * @return the buffer to send to server, or null to drop the packet
     */
    protected abstract ByteBuffer processClientToServer(ByteBuffer receiveBuffer,
            InetSocketAddress fromAddress, InetSocketAddress toAddress);

    /**
     * Processes a packet from server to client. Subclasses can inspect, modify, or
     * reject packets.
     *
     * @param receiveBuffer
     *            the received packet data
     * @param fromAddress
     *            the server's address
     * @param toAddress
     *            the client's address
     * @return the buffer to send to client, or null to drop the packet
     */
    protected abstract ByteBuffer processServerToClient(ByteBuffer receiveBuffer,
            InetSocketAddress fromAddress, InetSocketAddress toAddress);

    /**
     * Called when a new V086 relay should be created for a dynamically allocated
     * port. Subclasses can override this to spawn additional relay instances.
     *
     * @param port
     *            the port number for the new relay
     */
    protected void onDynamicPortAllocated(int port) {
        // Default: no-op. Subclasses can override.
    }

    // SmartLifecycle implementation

    @Override
    public void start() {
        if (running) {
            log.debug("{} start request ignored: already running", this);
            return;
        }

        try {
            listenChannel = DatagramChannel.open();
            listenChannel.socket()
                    .bind(new InetSocketAddress(InetAddress.getLocalHost(), listenPort));
            log.info("{} bound to port {}", this, listenPort);

            running = true;
            stopFlag = false;
            startTime.set(System.currentTimeMillis());

            threadPool.execute(this);
            log.info("{} started, relaying to {}", this, serverSocketAddress);
        } catch (Exception e) {
            log.error("Failed to start {}: {}", this, e.getMessage(), e);
            running = false;
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        log.info("{} stopping...", this);
        stopFlag = true;

        try {
            if (listenChannel != null) {
                listenChannel.close();
            }
        } catch (Exception e) {
            log.debug("Error closing listen channel during stop", e);
        }

        // Close all client handlers
        synchronized (clients) {
            for (ClientHandler handler : clients.values()) {
                handler.close();
            }
            clients.clear();
        }

        running = false;
        log.info("{} stopped", this);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // Phase 25: After domain model (20), before protocol handlers (28-30)
        return 25;
    }

    @Override
    public void run() {
        log.info("{} main thread running...", this);

        try {
            while (!stopFlag) {
                ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                InetSocketAddress clientAddress = (InetSocketAddress) listenChannel.receive(buffer);

                if (clientAddress == null) {
                    continue;
                }

                ClientHandler clientHandler = clients.get(clientAddress);
                if (clientHandler == null) {
                    if (clients.size() >= maxConnections) {
                        log.warn("Connection limit reached ({}), rejecting client {}",
                                maxConnections, EmuUtil.formatSocketAddress(clientAddress));
                        continue;
                    }

                    try {
                        clientHandler = new ClientHandler(clientAddress);
                    } catch (Exception e) {
                        log.error("Failed to start new ClientHandler for {}",
                                EmuUtil.formatSocketAddress(clientAddress), e);
                        continue;
                    }

                    clients.put(clientAddress, clientHandler);
                    totalConnections.incrementAndGet();
                    threadPool.execute(clientHandler);
                }

                buffer.flip();
                clientHandler.send(buffer);
            }
        } catch (Exception e) {
            if (!stopFlag) {
                log.error("{} main thread caught exception: {}", this, e.getMessage(), e);
            }
        } finally {
            try {
                if (listenChannel != null) {
                    listenChannel.close();
                }
            } catch (Exception e) {
                log.debug("Error closing listen channel", e);
            }
        }

        log.info("{} main thread exiting...", this);
    }

    /**
     * Handles bidirectional communication for a single client connection.
     */
    protected class ClientHandler implements Runnable {

        private final InetSocketAddress clientSocketAddress;
        private final DatagramChannel clientChannel;
        private final long connectTime;
        private volatile boolean closed = false;

        protected ClientHandler(InetSocketAddress clientSocketAddress) throws Exception {
            this.clientSocketAddress = clientSocketAddress;
            this.connectTime = System.currentTimeMillis();
            clientChannel = DatagramChannel.open();
            clientChannel.socket().bind(null);
            log.info("ClientHandler for {} bound to port {}",
                    EmuUtil.formatSocketAddress(clientSocketAddress),
                    clientChannel.socket().getPort());
        }

        /**
         * Returns the client's socket address.
         */
        public InetSocketAddress getClientSocketAddress() {
            return clientSocketAddress;
        }

        /**
         * Returns the connection time in milliseconds since epoch.
         */
        public long getConnectTime() {
            return connectTime;
        }

        /**
         * Sends a packet from client to server.
         */
        protected void send(ByteBuffer buffer) throws Exception {
            ByteBuffer newBuffer = processClientToServer(buffer, clientSocketAddress,
                    serverSocketAddress);
            if (newBuffer != null) {
                int bytes = clientChannel.send(newBuffer, serverSocketAddress);
                bytesRelayed.addAndGet(bytes);
            }
        }

        /**
         * Closes this client handler.
         */
        public void close() {
            closed = true;
            try {
                clientChannel.close();
            } catch (Exception e) {
                log.debug("Error closing client channel", e);
            }
        }

        @Override
        public void run() {
            log.info("ClientHandler thread for {} running...",
                    EmuUtil.formatSocketAddress(clientSocketAddress));

            try {
                while (!stopFlag && !closed) {
                    ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                    InetSocketAddress receiveAddress = (InetSocketAddress) clientChannel
                            .receive(buffer);

                    if (receiveAddress == null) {
                        continue;
                    }

                    if (!receiveAddress.getAddress().equals(serverSocketAddress.getAddress())) {
                        continue;
                    }

                    buffer.flip();

                    ByteBuffer newBuffer = processServerToClient(buffer, receiveAddress,
                            clientSocketAddress);
                    if (newBuffer != null) {
                        int bytes = listenChannel.send(newBuffer, clientSocketAddress);
                        bytesRelayed.addAndGet(bytes);
                    }
                }
            } catch (Exception e) {
                if (!stopFlag && !closed) {
                    log.info("ClientHandler thread for {} caught exception: {}",
                            EmuUtil.formatSocketAddress(clientSocketAddress), e.getMessage(), e);
                }
            } finally {
                try {
                    clientChannel.close();
                } catch (Exception e) {
                    log.debug("Error closing client channel", e);
                }

                clients.remove(clientSocketAddress);
            }

            log.info("ClientHandler thread for {} exiting...",
                    EmuUtil.formatSocketAddress(clientSocketAddress));
        }
    }
}
