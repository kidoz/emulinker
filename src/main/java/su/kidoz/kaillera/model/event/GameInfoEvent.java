package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraGame;

public final class GameInfoEvent implements GameEvent {
    private KailleraGame game;
    private String message;

    public GameInfoEvent(KailleraGame game, String message) {
        this.game = game;
        this.message = message;
    }

    public String toString() {
        return "GameInfoEvent";
    }

    public KailleraGame getGame() {
        return game;
    }

    public String getMessage() {
        return message;
    }
}
