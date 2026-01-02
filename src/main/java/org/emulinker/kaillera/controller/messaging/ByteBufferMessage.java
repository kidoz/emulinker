package org.emulinker.kaillera.controller.messaging;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ByteBufferMessage {
    protected static final Logger log = LoggerFactory.getLogger(ByteBufferMessage.class);

    // Default to UTF-8 for consistent cross-platform behavior.
    // Can be overridden via -Demulinker.charset=<charset> system property.
    public static Charset charset = StandardCharsets.UTF_8;

    static {
        String charsetName = System.getProperty("emulinker.charset");
        if (charsetName != null) {
            try {
                if (Charset.isSupported(charsetName)) {
                    charset = Charset.forName(charsetName);
                } else {
                    log.error("Charset {} is not supported, using UTF-8", charsetName);
                }
            } catch (Exception e) {
                log.error("Failed to load charset {}: {}, using UTF-8", charsetName,
                        e.getMessage());
            }
        }

        log.info("Using character set: {}", charset.displayName());
    }

    private ByteBuffer buffer;

    public abstract int getLength();

    public abstract String getDescription();

    public abstract String toString();

    protected void initBuffer() {
        initBuffer(getLength());
    }

    private void initBuffer(int size) {
        buffer = getBuffer(size);
    }

    public void releaseBuffer() {

    }

    public ByteBuffer toBuffer() {
        initBuffer();
        writeTo(buffer);
        buffer.flip();
        return buffer;
    }

    public abstract void writeTo(ByteBuffer buffer);

    public static ByteBuffer getBuffer(int size) {
        return ByteBuffer.allocateDirect(size);
    }

    public static void releaseBuffer(ByteBuffer buffer) {
        // nothing to do since we aren't caching buffers anymore
    }
}
