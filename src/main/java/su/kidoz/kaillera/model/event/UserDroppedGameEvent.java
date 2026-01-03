package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraUser;

public final class UserDroppedGameEvent implements GameEvent {
    private KailleraGame game;
    private KailleraUser user;
    private int playerNumber;

    public UserDroppedGameEvent(KailleraGame game, KailleraUser user, int playerNumber) {
        this.game = game;
        this.user = user;
        this.playerNumber = playerNumber;
    }

    public String toString() {
        return "UserDroppedGameEvent";
    }

    public KailleraGame getGame() {
        return game;
    }

    public KailleraUser getUser() {
        return user;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }
}
