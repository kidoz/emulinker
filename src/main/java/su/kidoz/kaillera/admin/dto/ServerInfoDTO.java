package su.kidoz.kaillera.admin.dto;

/**
 * DTO for server information response.
 */
public record ServerInfoDTO(String serverName, String version, int build, boolean running,
        int connectPort, long uptimeMinutes, int userCount, int maxUsers, int gameCount,
        int maxGames, StatsDTO stats, ThreadPoolDTO threadPool) {

    /**
     * Nested DTO for connection statistics.
     */
    public record StatsDTO(int requestCount, int connectCount, int protocolErrors, int deniedFull,
            int deniedOther) {
    }

    /**
     * Nested DTO for thread pool status.
     */
    public record ThreadPoolDTO(int active, long poolSize, int maxPoolSize, long taskCount) {
    }
}
