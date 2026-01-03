package su.kidoz.kaillera.master;

import java.util.List;

import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraServer;

public interface StatsCollector {
    void gameStarted(KailleraServer server, KailleraGame game);

    List<String> getStartedGamesList();

    void clearStartedGamesList();
}
