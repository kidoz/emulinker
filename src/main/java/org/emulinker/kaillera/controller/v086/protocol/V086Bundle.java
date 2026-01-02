package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.emulinker.kaillera.controller.messaging.ByteBufferMessage;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.messaging.ParseException;
import org.emulinker.util.EmuUtil;
import org.emulinker.util.UnsignedUtil;

public class V086Bundle extends ByteBufferMessage {
    public static final String DESC = "Kaillera v.086 Message Bundle";

    /** Minimum buffer length required to parse a bundle header */
    private static final int MIN_BUFFER_LENGTH = 5;

    /** Maximum number of messages allowed per bundle */
    private static final int MAX_MESSAGE_COUNT = 32;

    /** Minimum bytes per message header (message number + length) */
    private static final int MESSAGE_HEADER_SIZE = 6;

    protected V086Message[] messages;
    protected int numToWrite;
    protected int length = -1;

    public V086Bundle(V086Message[] messages) {
        this(messages, Integer.MAX_VALUE);
    }

    public V086Bundle(V086Message[] messages, int numToWrite) {
        this.numToWrite = messages.length;
        if (numToWrite < this.numToWrite)
            this.numToWrite = numToWrite;

        this.messages = messages;
    }

    public String getDescription() {
        return DESC;
    }

    public int getNumMessages() {
        return numToWrite;
    }

    public V086Message[] getMessages() {
        return messages;
    }

    public int getLength() {
        if (length == -1) {
            length = 0; // Initialize to 0 before summing
            for (int i = 0; i < numToWrite; i++) {
                if (messages[i] == null) {
                    break;
                }
                length += messages[i].getLength();
            }
        }
        return length;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(DESC + " (" + numToWrite + " messages) (" + getLength() + " bytes)");
        sb.append(EmuUtil.LB);
        for (int i = 0; i < numToWrite; i++) {
            if (messages[i] == null)
                break;

            sb.append("\tMessage " + (i + 1) + ": " + messages[i].toString() + EmuUtil.LB);
        }
        return sb.toString();
    }

    public void writeTo(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        // no real need for unsigned
        // UnsignedUtil.putUnsignedByte(buffer, numToWrite);
        buffer.put((byte) numToWrite);
        for (int i = 0; i < numToWrite; i++) {
            if (messages[i] == null)
                break;

            messages[i].writeTo(buffer);
        }
    }

    public static V086Bundle parse(ByteBuffer buffer)
            throws ParseException, V086BundleFormatException, MessageFormatException {
        return parse(buffer, -1);
    }

    public static V086Bundle parse(ByteBuffer buffer, int lastMessageID)
            throws ParseException, V086BundleFormatException, MessageFormatException {
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        if (buffer.limit() < MIN_BUFFER_LENGTH)
            throw new V086BundleFormatException("Invalid buffer length: " + buffer.limit());

        int messageCount = buffer.get();

        if (messageCount <= 0 || messageCount > MAX_MESSAGE_COUNT)
            throw new V086BundleFormatException("Invalid message count: " + messageCount);

        if (buffer.limit() < (1 + (messageCount * MESSAGE_HEADER_SIZE)))
            throw new V086BundleFormatException("Invalid bundle length: " + buffer.limit());

        V086Message[] messages = new V086Message[messageCount];
        int parsedCount;
        for (parsedCount = 0; parsedCount < messageCount; parsedCount++) {
            // Validate buffer has enough bytes for message header (2 bytes number + 2 bytes
            // length)
            if (buffer.remaining() < 4)
                throw new V086BundleFormatException(
                        "Bundle appears truncated, remaining = " + buffer.remaining());

            int messageNumber = UnsignedUtil.getUnsignedShort(buffer);
            // Note: messageNumber range check removed - getUnsignedShort always returns
            // 0-65535

            if (messageNumber <= lastMessageID) {
                // buffer.position((buffer.position() + messageLength));

                // will break here instead of looking for more messages, should improve speed
                // slightly
                // there shouldn't be any more valid messages anyway if we just found the last
                // ID
                break;
            }

            // no real need for unsigned
            short messageLength = buffer.getShort();
            if (messageLength < 2 || messageLength > buffer.remaining()
                    || messageLength > buffer.limit())
                throw new ParseException("Invalid message length: " + messageLength);

            // messages are purposely added in reverse order here so it's faster to read out
            // in the controller
            // messages.push(V086Message.parse(messageNumber, messageLength, buffer));
            // messages.addFirst(V086Message.parse(messageNumber, messageLength, buffer));
            // messages.add(V086Message.parse(messageNumber, messageLength, buffer));
            messages[parsedCount] = V086Message.parse(messageNumber, messageLength, buffer);
        }

        return new V086Bundle(messages, parsedCount);
    }
}
