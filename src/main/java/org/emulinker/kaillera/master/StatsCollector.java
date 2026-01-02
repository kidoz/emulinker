package org.emulinker.kaillera.master;

import java.util.List;

import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;

public interface StatsCollector {
    void gameStarted(KailleraServer server, KailleraGame game);

    List<String> getStartedGamesList();

    void clearStartedGamesList();
}
