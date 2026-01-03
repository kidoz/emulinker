package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraUser;

public final class UserJoinedGameEvent implements GameEvent {
    private KailleraGame game;
    private KailleraUser user;

    public UserJoinedGameEvent(KailleraGame game, KailleraUser user) {
        this.game = game;
        this.user = user;
    }

    public String toString() {
        return "UserJoinedGameEvent";
    }

    public KailleraGame getGame() {
        return game;
    }

    public KailleraUser getUser() {
        return user;
    }
}
