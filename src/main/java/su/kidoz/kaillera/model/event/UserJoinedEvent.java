package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraServer;
import su.kidoz.kaillera.model.KailleraUser;

public final class UserJoinedEvent implements ServerEvent {
    private KailleraServer server;
    private KailleraUser user;

    public UserJoinedEvent(KailleraServer server, KailleraUser user) {
        this.server = server;
        this.user = user;
    }

    public String toString() {
        return "UserJoinedEvent";
    }

    public KailleraServer getServer() {
        return server;
    }

    public KailleraUser getUser() {
        return user;
    }
}
