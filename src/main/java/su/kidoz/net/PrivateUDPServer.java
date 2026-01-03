package su.kidoz.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import su.kidoz.util.EmuUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PrivateUDPServer extends UDPServer {
    private static final Logger log = LoggerFactory.getLogger(PrivateUDPServer.class);

    private final InetAddress remoteAddress;
    private final AtomicReference<InetSocketAddress> remoteSocketAddress = new AtomicReference<>();

    public PrivateUDPServer(boolean shutdownOnExit, InetAddress remoteAddress) {
        super(shutdownOnExit);
        this.remoteAddress = remoteAddress;
    }

    public InetAddress getRemoteInetAddress() {
        return remoteAddress;
    }

    public InetSocketAddress getRemoteSocketAddress() {
        return remoteSocketAddress.get();
    }

    protected void handleReceived(ByteBuffer buffer, InetSocketAddress inboundSocketAddress) {
        // Atomically set remote address on first packet (thread-safe)
        remoteSocketAddress.compareAndSet(null, inboundSocketAddress);
        InetSocketAddress currentRemote = remoteSocketAddress.get();
        if (!inboundSocketAddress.equals(currentRemote)) {
            log.warn("Rejecting packet received from wrong address: "
                    + EmuUtil.formatSocketAddress(inboundSocketAddress) + " != "
                    + EmuUtil.formatSocketAddress(currentRemote));
            return;
        }

        handleReceived(buffer);
    }

    protected abstract void handleReceived(ByteBuffer buffer);

    protected void send(ByteBuffer buffer) {
        super.send(buffer, remoteSocketAddress.get());
    }
}
