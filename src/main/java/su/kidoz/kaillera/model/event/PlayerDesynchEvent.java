package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraUser;

public final class PlayerDesynchEvent implements GameEvent {
    private KailleraGame game;
    private KailleraUser user;
    private String message;

    public PlayerDesynchEvent(KailleraGame game, KailleraUser user, String message) {
        this.game = game;
        this.user = user;
        this.message = message;
    }

    public String toString() {
        return "GameDesynchEvent";
    }

    public KailleraGame getGame() {
        return game;
    }

    public KailleraUser getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }
}
