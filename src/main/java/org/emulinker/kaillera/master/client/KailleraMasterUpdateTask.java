package org.emulinker.kaillera.master.client;

import java.util.Iterator;
import java.util.List;

import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.master.PublicServerInformation;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

public class KailleraMasterUpdateTask implements MasterListUpdateTask {
    private static final Logger log = LoggerFactory.getLogger(KailleraMasterUpdateTask.class);
    private static final String KAILLERA_MASTER_URL = "http://www.kaillera.com/touch_server.php";

    private final PublicServerInformation publicInfo;
    private final ConnectController connectController;
    private final KailleraServer kailleraServer;
    private final StatsCollector statsCollector;
    private final RestClient restClient;

    public KailleraMasterUpdateTask(PublicServerInformation publicInfo,
            ConnectController connectController, KailleraServer kailleraServer,
            StatsCollector statsCollector, RestClient restClient) {
        this.publicInfo = publicInfo;
        this.connectController = connectController;
        this.kailleraServer = kailleraServer;
        this.statsCollector = statsCollector;
        this.restClient = restClient;
    }

    public void touchMaster() {
        List createdGamesList = statsCollector.getStartedGamesList();

        StringBuilder createdGames = new StringBuilder();
        synchronized (createdGamesList) {
            Iterator iter = createdGamesList.iterator();
            while (iter.hasNext()) {
                createdGames.append(iter.next());
                createdGames.append("|");
            }

            createdGamesList.clear();
        }

        StringBuilder waitingGames = new StringBuilder();
        for (KailleraGame game : kailleraServer.getGames()) {
            if (game.getStatus() != KailleraGame.STATUS_WAITING)
                continue;

            waitingGames.append(game.getID());
            waitingGames.append("|");
            waitingGames.append(game.getRomName());
            waitingGames.append("|");
            waitingGames.append(game.getOwner().getName());
            waitingGames.append("|");
            waitingGames.append(game.getOwner().getClientType());
            waitingGames.append("|");
            waitingGames.append(game.getNumPlayers());
            waitingGames.append("|");
        }

        String uri = UriComponentsBuilder.fromUriString(KAILLERA_MASTER_URL)
                .queryParam("servername", publicInfo.getServerName())
                .queryParam("port", connectController.getBindPort())
                .queryParam("nbusers", kailleraServer.getNumUsers())
                .queryParam("maxconn", kailleraServer.getMaxUsers()).queryParam("version", "0.86")
                .queryParam("nbgames", kailleraServer.getNumGames())
                .queryParam("location", publicInfo.getLocation())
                .queryParam("ip", publicInfo.getConnectAddress())
                .queryParam("url", publicInfo.getWebsite()).build().toUriString();

        try {
            restClient.get().uri(uri).header("Kaillera-games", createdGames.toString())
                    .header("Kaillera-wgames", waitingGames.toString()).retrieve()
                    .toBodilessEntity();

            log.info("Touching Kaillera Master done");
        } catch (Exception e) {
            log.error("Failed to touch Kaillera Master: " + e.getMessage());
        }
    }
}
