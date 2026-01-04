package su.kidoz.kaillera.relay;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.kidoz.config.RelayConfig;
import su.kidoz.kaillera.controller.connectcontroller.protocol.ConnectMessage;
import su.kidoz.kaillera.controller.connectcontroller.protocol.ConnectMessage_HELLO;
import su.kidoz.kaillera.controller.connectcontroller.protocol.ConnectMessage_HELLOD00D;
import su.kidoz.kaillera.controller.connectcontroller.protocol.ConnectMessage_TOO;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.net.UDPRelay;
import su.kidoz.util.EmuUtil;

/**
 * Spring-managed Kaillera connection relay controller.
 *
 * <p>
 * Handles the initial connection handshake protocol on the main relay port.
 * When a client connects and receives a port assignment from the backend
 * server, this controller spawns a {@link V086RelayController} to handle the
 * game protocol traffic on the assigned port.
 *
 * <p>
 * Protocol flow:
 * <ol>
 * <li>Client sends HELLO to this relay</li>
 * <li>Relay forwards HELLO to backend server</li>
 * <li>Backend responds with HELLOD00D containing port number</li>
 * <li>Relay spawns V086RelayController for that port</li>
 * <li>Relay forwards HELLOD00D to client</li>
 * <li>Client connects to V086RelayController on assigned port</li>
 * </ol>
 */
public class KailleraRelayController extends UDPRelay {

    private static final Logger log = LoggerFactory.getLogger(KailleraRelayController.class);

    private final ExecutorService threadPool;
    private final RelayConfig config;
    private final Map<Integer, V086RelayController> v086Relays = new ConcurrentHashMap<>();

    /**
     * Creates a new Kaillera relay controller.
     *
     * @param threadPool
     *            the executor service for handling connections
     * @param config
     *            the relay configuration
     */
    public KailleraRelayController(ExecutorService threadPool, RelayConfig config) {
        super(threadPool, config.getListenPort(),
                new InetSocketAddress(config.getBackendHost(), config.getBackendPort()),
                config.getMaxConnections(), config.getBufferSize());
        this.threadPool = threadPool;
        this.config = config;
    }

    /**
     * Returns all active V086 relay controllers.
     */
    public Map<Integer, V086RelayController> getV086Relays() {
        return v086Relays;
    }

    @Override
    public String toString() {
        return "KailleraRelayController(port=" + getListenPort() + ", backend="
                + getServerSocketAddress() + ")";
    }

    @Override
    public void stop() {
        // Stop all spawned V086 relays first
        for (V086RelayController relay : v086Relays.values()) {
            try {
                relay.stop();
            } catch (Exception e) {
                log.warn("Error stopping V086 relay on port {}: {}", relay.getListenPort(),
                        e.getMessage());
            }
        }
        v086Relays.clear();

        super.stop();
    }

    @Override
    protected ByteBuffer processClientToServer(ByteBuffer receiveBuffer,
            InetSocketAddress fromAddress, InetSocketAddress toAddress) {
        ConnectMessage inMessage;

        try {
            inMessage = ConnectMessage.parse(receiveBuffer);
        } catch (MessageFormatException e) {
            log.warn("Unrecognized message format from {}",
                    EmuUtil.formatSocketAddress(fromAddress), e);
            incrementParseErrors();
            return null;
        }

        log.debug("{} -> {}: {}", EmuUtil.formatSocketAddress(fromAddress),
                EmuUtil.formatSocketAddress(toAddress), inMessage);

        if (inMessage instanceof ConnectMessage_HELLO clientTypeMessage) {
            log.info("Client {} version: {}", EmuUtil.formatSocketAddress(fromAddress),
                    clientTypeMessage.getProtocol());
        } else {
            log.warn("Client {} sent unexpected message: {}",
                    EmuUtil.formatSocketAddress(fromAddress), inMessage);
            return null;
        }

        // Forward the original packet unchanged
        ByteBuffer sendBuffer = ByteBuffer.allocate(receiveBuffer.limit());
        receiveBuffer.rewind();
        sendBuffer.put(receiveBuffer);
        sendBuffer.flip();
        return sendBuffer;
    }

    @Override
    protected ByteBuffer processServerToClient(ByteBuffer receiveBuffer,
            InetSocketAddress fromAddress, InetSocketAddress toAddress) {
        ConnectMessage inMessage;

        try {
            inMessage = ConnectMessage.parse(receiveBuffer);
        } catch (MessageFormatException e) {
            log.warn("Unrecognized message format from backend", e);
            incrementParseErrors();
            return null;
        }

        log.debug("{} -> {}: {}", EmuUtil.formatSocketAddress(fromAddress),
                EmuUtil.formatSocketAddress(toAddress), inMessage);

        if (inMessage instanceof ConnectMessage_HELLOD00D portMsg) {
            int assignedPort = portMsg.getPort();
            log.info("Backend assigned port {} for client {}", assignedPort,
                    EmuUtil.formatSocketAddress(toAddress));

            // Spawn a V086 relay for this port if not already exists
            if (!v086Relays.containsKey(assignedPort)) {
                try {
                    InetSocketAddress backendAddress = new InetSocketAddress(
                            getServerSocketAddress().getAddress(), assignedPort);
                    V086RelayController v086Relay = new V086RelayController(threadPool,
                            assignedPort, backendAddress, config.getMaxConnections(),
                            config.getBufferSize());
                    v086Relay.start();
                    v086Relays.put(assignedPort, v086Relay);
                    log.info("Started V086 relay on port {} -> {}", assignedPort, backendAddress);
                } catch (Exception e) {
                    log.error("Failed to start V086 relay on port {}: {}", assignedPort,
                            e.getMessage(), e);
                    return null;
                }
            }
        } else if (inMessage instanceof ConnectMessage_TOO) {
            log.warn("Backend server is FULL for client {}",
                    EmuUtil.formatSocketAddress(toAddress));
        } else {
            log.warn("Backend sent unexpected message: {}", inMessage);
            return null;
        }

        // Forward the original packet unchanged
        ByteBuffer sendBuffer = ByteBuffer.allocate(receiveBuffer.limit());
        receiveBuffer.rewind();
        sendBuffer.put(receiveBuffer);
        sendBuffer.flip();
        return sendBuffer;
    }
}
