package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraServer;
import su.kidoz.kaillera.model.KailleraUser;

public final class ConnectedEvent implements UserEvent {
    private KailleraUser user;
    private KailleraServer server;

    public ConnectedEvent(KailleraServer server, KailleraUser user) {
        this.server = server;
        this.user = user;
    }

    public String toString() {
        return "ConnectedEvent";
    }

    public KailleraUser getUser() {
        return user;
    }

    public KailleraServer getServer() {
        return server;
    }
}
