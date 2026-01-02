package org.emulinker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Master list configuration properties.
 *
 * <p>
 * Maps to properties with prefix "masterList." in application.properties.
 */
@ConfigurationProperties(prefix = "master-list")
@Validated
public class MasterListConfig {

    private String serverName = "Kaillux Server";

    private String serverLocation = "Unknown";

    private String serverWebsite = "";

    private String serverConnectAddress = "";

    private boolean touchKaillera = false;

    private boolean touchEmulinker = false;

    private String emulinkerMasterUrl = "http://master.emulinker.org/touch.php";

    // Getters and setters

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerLocation() {
        return serverLocation;
    }

    public void setServerLocation(String serverLocation) {
        this.serverLocation = serverLocation;
    }

    public String getServerWebsite() {
        return serverWebsite;
    }

    public void setServerWebsite(String serverWebsite) {
        this.serverWebsite = serverWebsite;
    }

    public String getServerConnectAddress() {
        return serverConnectAddress;
    }

    public void setServerConnectAddress(String serverConnectAddress) {
        this.serverConnectAddress = serverConnectAddress;
    }

    public boolean isTouchKaillera() {
        return touchKaillera;
    }

    public void setTouchKaillera(boolean touchKaillera) {
        this.touchKaillera = touchKaillera;
    }

    public boolean isTouchEmulinker() {
        return touchEmulinker;
    }

    public void setTouchEmulinker(boolean touchEmulinker) {
        this.touchEmulinker = touchEmulinker;
    }

    public String getEmulinkerMasterUrl() {
        return emulinkerMasterUrl;
    }

    public void setEmulinkerMasterUrl(String emulinkerMasterUrl) {
        this.emulinkerMasterUrl = emulinkerMasterUrl;
    }
}
