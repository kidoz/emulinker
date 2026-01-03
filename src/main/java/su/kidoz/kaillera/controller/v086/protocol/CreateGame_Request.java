package su.kidoz.kaillera.controller.v086.protocol;

import su.kidoz.kaillera.controller.messaging.MessageFormatException;

public class CreateGame_Request extends CreateGame {
    public static final String DESC = "Create Game Request";

    public CreateGame_Request(int messageNumber, String romName) throws MessageFormatException {
        super(messageNumber, "", romName, "", 0xFFFF, 0xFFFF);
    }

    public byte getID() {
        return ID;
    }

    public String getDescription() {
        return DESC;
    }

    public String toString() {
        return getInfoString() + "[romName=" + getRomName() + "]";
    }
}
