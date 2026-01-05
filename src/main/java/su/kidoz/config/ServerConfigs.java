package su.kidoz.config;

/**
 * Groups all server configuration objects.
 *
 * <p>
 * This record bundles configuration properties for different aspects of the
 * server, including general server settings, game behavior, and master list
 * registration.
 *
 * @param server
 *            general server configuration (max users, timeouts, etc.)
 * @param game
 *            game-specific configuration (buffer size, timeouts, etc.)
 * @param masterList
 *            master server list registration settings
 */
public record ServerConfigs(ServerConfig server, GameConfig game, MasterListConfig masterList) {
}
