package org.emulinker.kaillera.master;

import org.emulinker.config.MasterListConfig;

public class PublicServerInformation {
    private final String serverName;
    private final String serverLocation;
    private final String serverWebsite;
    private final String serverAddress;

    public PublicServerInformation(MasterListConfig config) {
        serverName = config.getServerName();
        serverLocation = config.getServerLocation();
        serverWebsite = config.getServerWebsite();
        serverAddress = config.getServerConnectAddress();
    }

    public String getServerName() {
        return serverName;
    }

    public String getLocation() {
        return serverLocation;
    }

    public String getWebsite() {
        return serverWebsite;
    }

    public String getConnectAddress() {
        return serverAddress;
    }
}
