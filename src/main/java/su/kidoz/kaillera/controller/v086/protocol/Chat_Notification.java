package su.kidoz.kaillera.controller.v086.protocol;

import su.kidoz.kaillera.controller.messaging.MessageFormatException;

public class Chat_Notification extends Chat {
    public static final String DESC = "Chat Notification";

    public Chat_Notification(int messageNumber, String userName, String message)
            throws MessageFormatException {
        super(messageNumber, userName, message);
    }

    public String getDescription() {
        return DESC;
    }

    public String toString() {
        return getInfoString() + "[userName=" + getUserName() + " message=" + getMessage() + "]";
    }
}
