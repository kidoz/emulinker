package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraGame;

public final class AllReadyEvent implements GameEvent {
    private KailleraGame game;

    public AllReadyEvent(KailleraGame game) {
        this.game = game;
    }

    public String toString() {
        return "AllReadyEvent";
    }

    public KailleraGame getGame() {
        return game;
    }
}
