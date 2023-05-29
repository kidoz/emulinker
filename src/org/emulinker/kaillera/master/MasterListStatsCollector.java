package org.emulinker.kaillera.master;

import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MasterListStatsCollector implements StatsCollector
{
	private ArrayList<String> startedGamesList = new ArrayList<String>();

	public synchronized void gameStarted(KailleraServer server, KailleraGame game)
	{
		startedGamesList.add(game.getRomName());
	}

	public synchronized List<String> getStartedGamesList()
	{
		return startedGamesList;
	}

	public synchronized void clearStartedGamesList()
	{
		startedGamesList.clear();
	}
}
