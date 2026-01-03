package su.kidoz.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;

import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.messaging.ParseException;
import su.kidoz.util.EmuUtil;
import su.kidoz.util.UnsignedUtil;

public class GameData extends V086Message {
    public static final byte ID = 0x12;
    public static final String DESC = "Game Data";

    private byte[] gameData;

    public GameData(int messageNumber, byte[] gameData) throws MessageFormatException {
        super(messageNumber);

        if (gameData.length <= 0 || gameData.length > 0xFFFF)
            throw new MessageFormatException("Invalid " + getDescription()
                    + " format: gameData.remaining() = " + gameData.length);

        this.gameData = gameData;
    }

    public byte getID() {
        return ID;
    }

    public String getDescription() {
        return DESC;
    }

    public byte[] getGameData() {
        return gameData;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getInfoString());
        sb.append("[gameData=");
        sb.append(EmuUtil.arrayToString(gameData, ','));
        sb.append("]");
        return sb.toString();
    }

    public int getBodyLength() {
        return (gameData.length + 3);
    }

    public void writeBodyTo(ByteBuffer buffer) {
        buffer.put((byte) 0x00);
        UnsignedUtil.putUnsignedShort(buffer, gameData.length);
        buffer.put(gameData);
    }

    public static GameData parse(int messageNumber, ByteBuffer buffer)
            throws ParseException, MessageFormatException {
        if (buffer.remaining() < 4)
            throw new ParseException("Failed byte count validation!");

        buffer.get(); // Skip validation byte (removed to increase speed)
        // if (b != 0x00)
        // throw new MessageFormatException("Invalid " + DESC + " format: byte 0 = " +
        // EmuUtil.byteToHex(b));

        int dataSize = UnsignedUtil.getUnsignedShort(buffer);
        if (dataSize <= 0 || dataSize > buffer.remaining())
            throw new MessageFormatException("Invalid " + DESC + " format: dataSize = " + dataSize);

        byte[] gameData = new byte[dataSize];
        buffer.get(gameData);

        return new GameData(messageNumber, gameData);
    }
}
