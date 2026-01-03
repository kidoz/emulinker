package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraServer;

public final class GameCreatedEvent implements ServerEvent {
    private KailleraServer server;
    private KailleraGame game;

    public GameCreatedEvent(KailleraServer server, KailleraGame game) {
        this.server = server;
        this.game = game;
    }

    public String toString() {
        return "GameCreatedEvent";
    }

    public KailleraServer getServer() {
        return server;
    }

    public KailleraGame getGame() {
        return game;
    }
}
