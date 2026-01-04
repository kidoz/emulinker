package su.kidoz.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import su.kidoz.kaillera.relay.KailleraRelayController;
import su.kidoz.util.EmuLinkerExecutor;

/**
 * Spring configuration for relay mode.
 *
 * <p>
 * This configuration is only activated when {@code relay.enabled=true} is set
 * in the application properties. When enabled, it creates the necessary beans
 * to run the Kaillera server in relay mode, forwarding traffic to a backend
 * server.
 *
 * <p>
 * Relay mode is useful for:
 * <ul>
 * <li>Allowing clients behind NAT/firewalls to connect to Kaillera servers</li>
 * <li>Load balancing or proxying traffic to backend servers</li>
 * <li>Protocol inspection and debugging</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "relay.enabled", havingValue = "true")
@EnableConfigurationProperties(RelayConfig.class)
public class RelayConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RelayConfiguration.class);

    /**
     * Creates a scheduled executor for relay cleanup tasks.
     *
     * @return the scheduled executor service
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService relayCleanupScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "relay-cleanup");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Creates the main Kaillera relay controller.
     *
     * <p>
     * This controller handles the initial connection handshake on the configured
     * relay port and spawns V086 relay controllers for each dynamically assigned
     * game port.
     *
     * @param executor
     *            the executor service for handling connections
     * @param scheduler
     *            the scheduler for periodic cleanup tasks
     * @param config
     *            the relay configuration
     * @return the configured relay controller
     */
    @Bean
    public KailleraRelayController kailleraRelayController(EmuLinkerExecutor executor,
            ScheduledExecutorService scheduler, RelayConfig config) {
        log.info("Relay mode enabled: listening on port {}, forwarding to {}:{}",
                config.getListenPort(), config.getBackendHost(), config.getBackendPort());

        return new KailleraRelayController(executor, scheduler, config);
    }
}
