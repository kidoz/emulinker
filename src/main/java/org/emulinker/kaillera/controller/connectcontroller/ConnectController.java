package org.emulinker.kaillera.controller.connectcontroller;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.emulinker.config.ControllersConfig;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage;
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage_HELLO;
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage_HELLOD00D;
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage_PING;
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage_PONG;
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage_TOO;
import org.emulinker.kaillera.controller.messaging.ByteBufferMessage;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.model.exception.NewConnectionException;
import org.emulinker.kaillera.model.exception.ServerFullException;
import org.emulinker.net.BindException;
import org.emulinker.net.MultiAddressUDPServer;
import org.emulinker.util.EmuLinkerExecutor;
import org.emulinker.util.EmuUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectController extends MultiAddressUDPServer {
    private static final Logger log = LoggerFactory.getLogger(ConnectController.class);

    private final EmuLinkerExecutor threadPool;
    private final AccessManager accessManager;
    private final Map<String, KailleraServerController> controllersMap;

    private final int bufferSize;

    private final AtomicLong startTime = new AtomicLong(0);
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicInteger messageFormatErrorCount = new AtomicInteger(0);
    private final AtomicInteger protocolErrorCount = new AtomicInteger(0);
    private final AtomicInteger deniedServerFullCount = new AtomicInteger(0);
    private final AtomicInteger deniedOtherCount = new AtomicInteger(0);
    private final AtomicInteger failedToStartCount = new AtomicInteger(0);
    private final AtomicInteger connectedCount = new AtomicInteger(0);
    private final AtomicInteger pingCount = new AtomicInteger(0);

    public ConnectController(EmuLinkerExecutor threadPool,
            KailleraServerController[] controllersArray, AccessManager accessManager,
            ControllersConfig config) throws BindException {
        super(true);

        this.threadPool = threadPool;
        this.accessManager = accessManager;
        setExecutor(threadPool);

        int port = config.getConnect().getPort();
        this.bufferSize = config.getConnect().getBufferSize();
        List<InetAddress> bindAddresses = config.getParsedBindAddresses();

        controllersMap = new HashMap<String, KailleraServerController>();
        for (KailleraServerController controller : controllersArray) {
            String[] clientTypes = controller.getClientTypes();
            for (int j = 0; j < clientTypes.length; j++) {
                log.debug("Mapping client type " + clientTypes[j] + " to " + controller);
                controllersMap.put(clientTypes[j], controller);
            }
        }

        super.bind(port, bindAddresses);

        log.info("Ready to accept connections on port {} (addresses: {})", port,
                bindAddresses.stream().map(InetAddress::getHostAddress).toList());
    }

    public KailleraServerController getController(String clientType) {
        return controllersMap.get(clientType);
    }

    public Collection<KailleraServerController> getControllers() {
        return controllersMap.values();
    }

    public long getStartTime() {
        return startTime.get();
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getRequestCount() {
        return requestCount.get();
    }

    public int getMessageFormatErrorCount() {
        return messageFormatErrorCount.get();
    }

    public int getProtocolErrorCount() {
        return protocolErrorCount.get();
    }

    public int getDeniedServerFullCount() {
        return deniedServerFullCount.get();
    }

    public int getDeniedOtherCount() {
        return deniedOtherCount.get();
    }

    public int getFailedToStartCount() {
        return failedToStartCount.get();
    }

    public int getConnectCount() {
        return connectedCount.get();
    }

    public int getPingCount() {
        return pingCount.get();
    }

    protected ByteBuffer getBuffer() {
        return ByteBufferMessage.getBuffer(bufferSize);
    }

    protected void releaseBuffer(ByteBuffer buffer) {
        ByteBufferMessage.releaseBuffer(buffer);
    }

    public String toString() {
        if (getBindPort() > 0)
            return "ConnectController(" + getBindPort() + ")";
        else
            return "ConnectController(unbound)";
    }

    public synchronized void start() {
        if (isRunning()) {
            log.debug(this + " start request ignored: already running!");
            return;
        }

        super.start();
        startTime.set(System.currentTimeMillis());
        log.debug(this + " Starting handlers (ThreadPool:" + threadPool.getActiveCount() + "/"
                + threadPool.getPoolSize() + ")");
        // run() starts all channel handlers using the executor
        this.run();
        log.debug(this + " Handlers started (ThreadPool:" + threadPool.getActiveCount() + "/"
                + threadPool.getPoolSize() + ")");
    }

    public synchronized void stop() {
        super.stop();
        for (KailleraServerController controller : controllersMap.values())
            controller.stop();
    }

    protected synchronized void handleReceived(ByteBuffer buffer,
            InetSocketAddress fromSocketAddress) {
        requestCount.incrementAndGet();

        ConnectMessage inMessage;

        try {
            inMessage = ConnectMessage.parse(buffer);
        } catch (MessageFormatException e) {
            messageFormatErrorCount.incrementAndGet();
            buffer.rewind();
            log.warn("Received invalid message from "
                    + EmuUtil.formatSocketAddress(fromSocketAddress) + ": "
                    + EmuUtil.dumpBuffer(buffer));
            return;
        }

        // the message set of the ConnectController isn't really complex enough to
        // warrant a complicated request/action class
        // structure, so I'm going to handle it all in this class alone

        if (inMessage instanceof ConnectMessage_PING) {
            pingCount.incrementAndGet();
            log.debug("Ping from: " + EmuUtil.formatSocketAddress(fromSocketAddress));
            send(new ConnectMessage_PONG(), fromSocketAddress);
            return;
        }

        if (!(inMessage instanceof ConnectMessage_HELLO connectMessage)) {
            messageFormatErrorCount.incrementAndGet();
            log.warn("Received unexpected message type from "
                    + EmuUtil.formatSocketAddress(fromSocketAddress) + ": " + inMessage);
            return;
        }

        // now we need to find the specific server this client is request to
        // connect to using the client type
        KailleraServerController protocolController = getController(connectMessage.getProtocol());
        if (protocolController == null) {
            protocolErrorCount.incrementAndGet();
            log.error("Client requested an unhandled protocol "
                    + EmuUtil.formatSocketAddress(fromSocketAddress) + ": "
                    + connectMessage.getProtocol());
            return;
        }

        if (!accessManager.isAddressAllowed(fromSocketAddress.getAddress())) {
            deniedOtherCount.incrementAndGet();
            log.warn("AccessManager denied connection from "
                    + EmuUtil.formatSocketAddress(fromSocketAddress));
        } else {
            int privatePort;

            try {
                privatePort = protocolController.newConnection(fromSocketAddress,
                        connectMessage.getProtocol());

                if (privatePort <= 0) {
                    failedToStartCount.incrementAndGet();
                    log.error(protocolController + " failed to start for "
                            + EmuUtil.formatSocketAddress(fromSocketAddress));
                    return;
                }

                connectedCount.incrementAndGet();
                log.debug(protocolController + " allocated port " + privatePort + " to client from "
                        + fromSocketAddress.getAddress().getHostAddress());
                send(new ConnectMessage_HELLOD00D(privatePort), fromSocketAddress);
            } catch (ServerFullException e) {
                deniedServerFullCount.incrementAndGet();
                log.debug("Sending server full response to "
                        + EmuUtil.formatSocketAddress(fromSocketAddress));
                send(new ConnectMessage_TOO(), fromSocketAddress);
            } catch (NewConnectionException e) {
                deniedOtherCount.incrementAndGet();
                log.warn(protocolController + " denied connection from "
                        + EmuUtil.formatSocketAddress(fromSocketAddress) + ": " + e.getMessage());
            }
        }
    }

    protected void send(ConnectMessage outMessage, InetSocketAddress toSocketAddress) {
        send(outMessage.toBuffer(), toSocketAddress);
        outMessage.releaseBuffer();
    }
}
