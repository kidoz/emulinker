package su.kidoz.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import su.kidoz.util.EmuUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UDPRelay implements Runnable {
    protected static final Logger log = LoggerFactory.getLogger(UDPRelay.class);

    private static final int DEFAULT_MAX_CONNECTIONS = 100;
    private static final int BUFFER_SIZE = 2048;

    // Instance-level thread pool to avoid shutdown affecting other instances
    private final ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();

    protected DatagramChannel listenChannel;

    protected int listenPort;
    protected InetSocketAddress serverSocketAddress;
    protected int maxConnections = DEFAULT_MAX_CONNECTIONS;
    protected volatile boolean stopFlag = false;

    protected Map<InetSocketAddress, ClientHandler> clients = Collections
            .synchronizedMap(new HashMap<InetSocketAddress, ClientHandler>());

    public UDPRelay(int listenPort, InetSocketAddress serverSocketAddress) throws Exception {
        this.listenPort = listenPort;
        this.serverSocketAddress = serverSocketAddress;

        listenChannel = DatagramChannel.open();
        listenChannel.socket()
                .bind(new InetSocketAddress(InetAddress.getLocalHost(), this.listenPort));

        log.info("Bound to port {}", listenPort);

        threadPool.execute(this);
    }

    public int getListenPort() {
        return listenPort;
    }
    public DatagramChannel getListenChannel() {
        return listenChannel;
    }
    public InetSocketAddress getServerSocketAddress() {
        return serverSocketAddress;
    }

    protected abstract ByteBuffer processClientToServer(ByteBuffer receiveBuffer,
            InetSocketAddress fromAddress, InetSocketAddress toAddress);
    protected abstract ByteBuffer processServerToClient(ByteBuffer receiveBuffer,
            InetSocketAddress fromAddress, InetSocketAddress toAddress);

    public void stop() {
        stopFlag = true;
        try {
            listenChannel.close();
        } catch (Exception e) {
            log.debug("Error closing listen channel during stop", e);
        }
    }

    public void run() {
        log.info("Main port {} thread running...", listenPort);

        try {
            while (!stopFlag) {
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                InetSocketAddress clientAddress = (InetSocketAddress) listenChannel.receive(buffer);

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
                    threadPool.execute(clientHandler);
                }

                buffer.flip();
                clientHandler.send(buffer);
            }
        } catch (Exception e) {
            if (!stopFlag) {
                log.error("Main port {} thread caught exception: {}", listenPort, e.getMessage(),
                        e);
            }
        } finally {
            try {
                listenChannel.close();
            } catch (Exception e) {
                log.debug("Error closing listen channel", e);
            }

            threadPool.shutdownNow();
        }

        log.info("Main port {} thread exiting...", listenPort);
    }

    protected class ClientHandler implements Runnable {
        protected InetSocketAddress clientSocketAddress;
        protected DatagramChannel clientChannel;

        protected ClientHandler(InetSocketAddress clientSocketAddress) throws Exception {
            this.clientSocketAddress = clientSocketAddress;
            clientChannel = DatagramChannel.open();
            clientChannel.socket().bind(null);
            log.info("ClientHandler for {} bound to port {}",
                    EmuUtil.formatSocketAddress(clientSocketAddress),
                    clientChannel.socket().getPort());
        }

        protected void send(ByteBuffer buffer) throws Exception {
            // log.info(EmuUtil.formatSocketAddress(clientSocketAddress) + " -> \t" +
            // EmuUtil.dumpBuffer(buffer));
            ByteBuffer newBuffer = processClientToServer(buffer, clientSocketAddress,
                    serverSocketAddress);
            clientChannel.send(newBuffer, serverSocketAddress);
        }

        public void run() {
            log.info("ClientHandler thread for {} running...",
                    EmuUtil.formatSocketAddress(clientSocketAddress));

            try {
                while (!stopFlag) {
                    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                    InetSocketAddress receiveAddress = (InetSocketAddress) clientChannel
                            .receive(buffer);

                    if (!receiveAddress.getAddress().equals(serverSocketAddress.getAddress()))
                        continue;

                    buffer.flip();

                    // log.info(EmuUtil.formatSocketAddress(clientSocketAddress) + " <- \t" +
                    // EmuUtil.dumpBuffer(buffer));
                    ByteBuffer newBuffer = processServerToClient(buffer, receiveAddress,
                            clientSocketAddress);
                    listenChannel.send(newBuffer, clientSocketAddress);
                }
            } catch (Exception e) {
                if (!stopFlag) {
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
