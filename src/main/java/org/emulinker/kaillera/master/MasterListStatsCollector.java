package org.emulinker.kaillera.master;

import java.util.ArrayList;
import java.util.List;

import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;

public class MasterListStatsCollector implements StatsCollector {
    private ArrayList<String> startedGamesList = new ArrayList<String>();

    public synchronized void gameStarted(KailleraServer server, KailleraGame game) {
        startedGamesList.add(game.getRomName());
    }

    public synchronized List<String> getStartedGamesList() {
        return startedGamesList;
    }

    public synchronized void clearStartedGamesList() {
        startedGamesList.clear();
    }
}
