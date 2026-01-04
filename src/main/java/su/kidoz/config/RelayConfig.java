package su.kidoz.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Relay mode configuration properties.
 *
 * <p>
 * When enabled, the server acts as a transparent UDP relay/proxy, forwarding
 * Kaillera protocol traffic between clients and a backend server. This enables
 * clients behind NAT/firewalls to connect to Kaillera servers they couldn't
 * reach directly.
 *
 * <p>
 * Maps to properties with prefix "relay." in application.properties.
 */
@ConfigurationProperties(prefix = "relay")
@Validated
public class RelayConfig {

    /**
     * Whether relay mode is enabled. When true, the server will start a relay
     * controller that forwards traffic to the configured backend server.
     */
    private boolean enabled = false;

    /**
     * The port to listen on for incoming relay connections. Should be different
     * from the main server's connect port.
     */
    @Min(1)
    @Max(65535)
    private int listenPort = 27887;

    /**
     * The hostname or IP address of the backend Kaillera server to relay traffic
     * to.
     */
    @NotBlank
    private String backendHost = "localhost";

    /**
     * The port of the backend Kaillera server's connect controller.
     */
    @Min(1)
    @Max(65535)
    private int backendPort = 27888;

    /**
     * Maximum number of concurrent relay connections allowed.
     */
    @Min(1)
    @Max(10000)
    private int maxConnections = 100;

    /**
     * Buffer size in bytes for UDP packets.
     */
    @Min(512)
    @Max(65536)
    private int bufferSize = 2048;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public String getBackendHost() {
        return backendHost;
    }

    public void setBackendHost(String backendHost) {
        this.backendHost = backendHost;
    }

    public int getBackendPort() {
        return backendPort;
    }

    public void setBackendPort(int backendPort) {
        this.backendPort = backendPort;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
