package su.kidoz.kaillera.controller.v086.protocol;

import su.kidoz.kaillera.controller.messaging.MessageFormatException;

public class StartGame_Request extends StartGame {
    public static final String DESC = "Start Game Request";

    public StartGame_Request(int messageNumber) throws MessageFormatException {
        super(messageNumber, 0xFFFF, (short) 0xFF, (short) 0xFF);
    }

    public String getDescription() {
        return DESC;
    }

    public String toString() {
        return getInfoString();
    }
}
