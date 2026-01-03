package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraGame;

public final class GameDataEvent implements GameEvent {
    private KailleraGame game;
    private byte[] data;

    public GameDataEvent(KailleraGame game, byte[] data) {
        this.game = game;
        this.data = data;
    }

    public String toString() {
        return "GameDataEvent";
    }

    public KailleraGame getGame() {
        return game;
    }

    public byte[] getData() {
        return data;
    }
}
