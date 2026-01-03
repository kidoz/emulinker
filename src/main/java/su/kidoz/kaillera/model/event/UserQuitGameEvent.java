package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraUser;

public final class UserQuitGameEvent implements GameEvent {
    private KailleraGame game;
    private KailleraUser user;

    public UserQuitGameEvent(KailleraGame game, KailleraUser user) {
        this.game = game;
        this.user = user;
    }

    public String toString() {
        return "UserQuitGameEvent";
    }

    public KailleraGame getGame() {
        return game;
    }

    public KailleraUser getUser() {
        return user;
    }
}
