package org.emulinker.kaillera.load;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple UDP-based Kaillera v0.86 client for load testing.
 *
 * <p>
 * This client implements the bare minimum of the Kaillera protocol to:
 * <ul>
 * <li>Connect to the server (HELLO handshake)</li>
 * <li>Login with user information</li>
 * <li>Send chat messages</li>
 * <li>Respond to keep-alives</li>
 * <li>Quit gracefully</li>
 * </ul>
 */
public final class UdpKailleraClient implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(UdpKailleraClient.class);

    // Protocol constants
    private static final String HELLO_PREFIX = "HELLO";
    private static final String HELLOD00D_PREFIX = "HELLOD00D";
    private static final String PROTOCOL_VERSION = "0.83";

    // Message type IDs
    private static final byte MSG_QUIT = 0x01;
    private static final byte MSG_USER_INFO = 0x03;
    private static final byte MSG_SERVER_ACK = 0x05;
    private static final byte MSG_CLIENT_ACK = 0x06;
    private static final byte MSG_CHAT = 0x07;
    private static final byte MSG_KEEP_ALIVE = 0x09;

    // Connection types
    public static final byte CONNECTION_LAN = 1;
    public static final byte CONNECTION_EXCELLENT = 2;
    public static final byte CONNECTION_GOOD = 3;
    public static final byte CONNECTION_AVERAGE = 4;
    public static final byte CONNECTION_LOW = 5;
    public static final byte CONNECTION_BAD = 6;

    private final String serverHost;
    private final int connectPort;
    private final String clientName;
    private final String clientType;
    private final byte connectionType;

    private DatagramChannel channel;
    private InetSocketAddress serverAddress;
    private int assignedPort;
    private final AtomicInteger messageNumber = new AtomicInteger(0);
    private volatile boolean connected = false;
    private volatile boolean loggedIn = false;

    private final ByteBuffer sendBuffer = ByteBuffer.allocate(2048);
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(2048);

    public UdpKailleraClient(String serverHost, int connectPort, String clientName) {
        this(serverHost, connectPort, clientName, "LoadTestClient/1.0", CONNECTION_LAN);
    }

    public UdpKailleraClient(String serverHost, int connectPort, String clientName,
            String clientType, byte connectionType) {
        this.serverHost = serverHost;
        this.connectPort = connectPort;
        this.clientName = clientName;
        this.clientType = clientType;
        this.connectionType = connectionType;
    }

    /**
     * Connects to the server and performs the HELLO handshake.
     *
     * @param timeoutMs
     *            timeout in milliseconds
     * @return true if connected successfully
     */
    public boolean connect(long timeoutMs) throws IOException {
        // Use IPv4 explicitly to avoid IPv6 issues
        channel = DatagramChannel.open(java.net.StandardProtocolFamily.INET);
        channel.configureBlocking(true);
        channel.socket().setSoTimeout((int) timeoutMs);

        // Bind to a local port so we can receive responses
        channel.bind(null);
        log.debug("{}: Bound to local port {}", clientName,
                ((InetSocketAddress) channel.getLocalAddress()).getPort());

        // Send HELLO to connect port
        InetSocketAddress connectAddress = new InetSocketAddress(serverHost, connectPort);
        sendBuffer.clear();
        sendBuffer
                .put((HELLO_PREFIX + PROTOCOL_VERSION + "\0").getBytes(StandardCharsets.US_ASCII));
        sendBuffer.flip();
        channel.send(sendBuffer, connectAddress);

        log.debug("{}: Sent HELLO to {}:{}", clientName, serverHost, connectPort);

        // Receive HELLOD00D with assigned port
        receiveBuffer.clear();
        java.net.SocketAddress responseAddr = channel.receive(receiveBuffer);
        receiveBuffer.flip();

        if (receiveBuffer.remaining() == 0) {
            log.error("{}: No response received (timeout)", clientName);
            return false;
        }

        log.debug("{}: Received {} bytes from {}", clientName, receiveBuffer.remaining(),
                responseAddr);

        byte[] responseBytes = new byte[receiveBuffer.remaining()];
        receiveBuffer.get(responseBytes);
        String response = new String(responseBytes, StandardCharsets.US_ASCII);

        log.debug("{}: Response string: {}", clientName, response.replace("\0", "\\0"));

        if (!response.startsWith(HELLOD00D_PREFIX)) {
            log.error("{}: Invalid response: {}", clientName, response);
            return false;
        }

        // Parse assigned port
        String portStr = response.substring(HELLOD00D_PREFIX.length());
        if (portStr.endsWith("\0")) {
            portStr = portStr.substring(0, portStr.length() - 1);
        }
        assignedPort = Integer.parseInt(portStr);

        serverAddress = new InetSocketAddress(serverHost, assignedPort);
        connected = true;

        log.debug("{}: Connected, assigned port {}", clientName, assignedPort);
        return true;
    }

    /**
     * Logs in to the server with user information.
     *
     * @param timeoutMs
     *            timeout in milliseconds
     * @return true if login successful
     */
    public boolean login(long timeoutMs) throws IOException {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }

        channel.socket().setSoTimeout((int) timeoutMs);

        // Send UserInformation message
        sendUserInformation();

        // Wait for ServerACK
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (receiveAndProcess()) {
                if (loggedIn) {
                    return true;
                }
            }
        }

        return loggedIn;
    }

    /**
     * Sends a chat message.
     */
    public void chat(String message) throws IOException {
        if (!loggedIn) {
            throw new IllegalStateException("Not logged in");
        }

        sendBuffer.clear();
        sendBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Bundle header: 1 message
        sendBuffer.put((byte) 1);

        // Message header
        int msgNum = messageNumber.incrementAndGet() & 0xFFFF;
        sendBuffer.putShort((short) msgNum);

        // Message body
        byte[] msgBytes = (message + "\0").getBytes(StandardCharsets.US_ASCII);
        int bodyLength = 3 + msgBytes.length; // username(empty) + \0 + message + \0
        sendBuffer.putShort((short) bodyLength);
        sendBuffer.put(MSG_CHAT);
        sendBuffer.put((byte) 0x00); // empty username (server fills it in)
        sendBuffer.put(msgBytes);

        sendBuffer.flip();
        channel.send(sendBuffer, serverAddress);
    }

    /**
     * Sends a quit message and closes the connection.
     */
    public void quit(String message) throws IOException {
        if (!connected) {
            return;
        }

        try {
            sendBuffer.clear();
            sendBuffer.order(ByteOrder.LITTLE_ENDIAN);

            // Bundle header: 1 message
            sendBuffer.put((byte) 1);

            // Message header
            int msgNum = messageNumber.incrementAndGet() & 0xFFFF;
            sendBuffer.putShort((short) msgNum);

            // Message body
            byte[] msgBytes = (message + "\0").getBytes(StandardCharsets.US_ASCII);
            int bodyLength = 3 + msgBytes.length; // userID (2) + message + \0
            sendBuffer.putShort((short) bodyLength);
            sendBuffer.put(MSG_QUIT);
            sendBuffer.putShort((short) 0xFFFF); // userID (server fills it)
            sendBuffer.put(msgBytes);

            sendBuffer.flip();
            channel.send(sendBuffer, serverAddress);

            log.debug("{}: Sent QUIT", clientName);
        } finally {
            connected = false;
            loggedIn = false;
        }
    }

    /**
     * Receives and processes incoming messages.
     *
     * @return true if a message was processed
     */
    public boolean receiveAndProcess() throws IOException {
        receiveBuffer.clear();
        try {
            channel.receive(receiveBuffer);
        } catch (java.net.SocketTimeoutException e) {
            return false;
        }
        receiveBuffer.flip();

        if (receiveBuffer.remaining() < 5) {
            return false;
        }

        receiveBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int messageCount = receiveBuffer.get() & 0xFF;

        for (int i = 0; i < messageCount && receiveBuffer.remaining() >= 5; i++) {
            int msgNum = receiveBuffer.getShort() & 0xFFFF;
            int msgLen = receiveBuffer.getShort() & 0xFFFF;
            byte msgType = receiveBuffer.get();

            int bodyLen = msgLen - 1;
            if (bodyLen > receiveBuffer.remaining()) {
                break;
            }

            switch (msgType) {
                case MSG_SERVER_ACK :
                    log.debug("{}: Received ServerACK #{}", clientName, msgNum);
                    // Skip the ACK body (17 bytes)
                    receiveBuffer.position(receiveBuffer.position() + Math.min(bodyLen, 17));
                    // Send ClientACK in response
                    sendClientAck(msgNum);
                    loggedIn = true;
                    break;

                case MSG_KEEP_ALIVE :
                    log.debug("{}: Received KeepAlive #{}", clientName, msgNum);
                    receiveBuffer.position(receiveBuffer.position() + bodyLen);
                    // Respond with keep-alive
                    sendKeepAlive();
                    break;

                default :
                    // Skip other messages
                    receiveBuffer.position(receiveBuffer.position() + bodyLen);
                    break;
            }
        }

        return true;
    }

    private void sendUserInformation() throws IOException {
        sendBuffer.clear();
        sendBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Bundle header: 1 message
        sendBuffer.put((byte) 1);

        // Message header
        int msgNum = messageNumber.incrementAndGet() & 0xFFFF;
        sendBuffer.putShort((short) msgNum);

        // Calculate body length
        byte[] nameBytes = (clientName + "\0").getBytes(StandardCharsets.US_ASCII);
        byte[] typeBytes = (clientType + "\0").getBytes(StandardCharsets.US_ASCII);
        int bodyLength = nameBytes.length + typeBytes.length + 1; // +1 for connectionType

        sendBuffer.putShort((short) bodyLength);
        sendBuffer.put(MSG_USER_INFO);
        sendBuffer.put(nameBytes);
        sendBuffer.put(typeBytes);
        sendBuffer.put(connectionType);

        sendBuffer.flip();
        channel.send(sendBuffer, serverAddress);

        log.debug("{}: Sent UserInformation", clientName);
    }

    private void sendClientAck(int ackMsgNum) throws IOException {
        sendBuffer.clear();
        sendBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Bundle header: 1 message
        sendBuffer.put((byte) 1);

        // Message header
        int msgNum = messageNumber.incrementAndGet() & 0xFFFF;
        sendBuffer.putShort((short) msgNum);
        sendBuffer.putShort((short) 18); // body length: 1 + 4*4 = 17, total = 18
        sendBuffer.put(MSG_CLIENT_ACK);

        // ACK body: 0x00 followed by 0, 1, 2, 3 as unsigned ints
        sendBuffer.put((byte) 0x00);
        sendBuffer.putInt(0);
        sendBuffer.putInt(1);
        sendBuffer.putInt(2);
        sendBuffer.putInt(3);

        sendBuffer.flip();
        channel.send(sendBuffer, serverAddress);

        log.debug("{}: Sent ClientACK", clientName);
    }

    private void sendKeepAlive() throws IOException {
        sendBuffer.clear();
        sendBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Bundle header: 1 message
        sendBuffer.put((byte) 1);

        // Message header
        int msgNum = messageNumber.incrementAndGet() & 0xFFFF;
        sendBuffer.putShort((short) msgNum);
        sendBuffer.putShort((short) 1); // body length: just the message type
        sendBuffer.put(MSG_KEEP_ALIVE);

        sendBuffer.flip();
        channel.send(sendBuffer, serverAddress);
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getClientName() {
        return clientName;
    }

    public int getAssignedPort() {
        return assignedPort;
    }

    @Override
    public void close() throws IOException {
        try {
            if (connected) {
                quit("Client closing");
            }
        } finally {
            if (channel != null) {
                channel.close();
            }
        }
    }
}
