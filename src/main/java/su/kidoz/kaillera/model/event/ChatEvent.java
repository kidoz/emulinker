package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraServer;
import su.kidoz.kaillera.model.KailleraUser;

public final class ChatEvent implements ServerEvent {
    private KailleraServer server;
    private KailleraUser user;
    private String message;

    public ChatEvent(KailleraServer server, KailleraUser user, String message) {
        this.server = server;
        this.user = user;
        this.message = message;
    }

    public String toString() {
        return "ChatEvent";
    }

    public KailleraServer getServer() {
        return server;
    }

    public KailleraUser getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }
}
