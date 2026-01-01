package org.emulinker.kaillera.master.client;

import java.io.StringReader;
import java.util.Properties;

import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.master.PublicServerInformation;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.EmuUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

public class EmuLinkerMasterUpdateTask implements MasterListUpdateTask {
    private static final Logger log = LoggerFactory.getLogger(EmuLinkerMasterUpdateTask.class);
    private static final String EMULINKER_MASTER_URL = "http://master.emulinker.org/touch.php";

    private final PublicServerInformation publicInfo;
    private final ConnectController connectController;
    private final KailleraServer kailleraServer;
    private final ReleaseInfo releaseInfo;
    private final RestClient restClient;

    public EmuLinkerMasterUpdateTask(PublicServerInformation publicInfo,
            ConnectController connectController, KailleraServer kailleraServer,
            ReleaseInfo releaseInfo, RestClient restClient) {
        this.publicInfo = publicInfo;
        this.connectController = connectController;
        this.kailleraServer = kailleraServer;
        this.releaseInfo = releaseInfo;
        this.restClient = restClient;
    }

    public void touchMaster() {
        String uri = UriComponentsBuilder.fromUriString(EMULINKER_MASTER_URL)
                .queryParam("serverName", publicInfo.getServerName())
                .queryParam("connectAddress", publicInfo.getConnectAddress())
                .queryParam("location", publicInfo.getLocation())
                .queryParam("website", publicInfo.getWebsite())
                .queryParam("port", connectController.getBindPort())
                .queryParam("connectCount", connectController.getConnectCount())
                .queryParam("numUsers", kailleraServer.getNumUsers())
                .queryParam("maxUsers", kailleraServer.getMaxUsers())
                .queryParam("numGames", kailleraServer.getNumGames())
                .queryParam("maxGames", kailleraServer.getMaxGames())
                .queryParam("version", releaseInfo.getVersionString())
                .queryParam("build", releaseInfo.getBuildNumber())
                .queryParam("isWindows", EmuUtil.systemIsWindows()).build().toUriString();

        Properties props = new Properties();

        try {
            String response = restClient.get().uri(uri).retrieve().body(String.class);

            if (response != null) {
                props.load(new StringReader(response));
            }

            log.info("Touching EmuLinker Master done");
        } catch (Exception e) {
            log.error("Failed to touch EmuLinker Master: " + e.getMessage());
        }

        String updateAvailable = props.getProperty("updateAvailable");
        if (updateAvailable != null && updateAvailable.equalsIgnoreCase("true")) {
            String latestVersion = props.getProperty("latest");
            String notes = props.getProperty("notes");
            StringBuilder sb = new StringBuilder();
            sb.append("A updated version of EmuLinker is available: ");
            sb.append(latestVersion);
            if (notes != null) {
                sb.append(" (");
                sb.append(notes);
                sb.append(")");
            }
            log.warn(sb.toString());
        }
    }
}
