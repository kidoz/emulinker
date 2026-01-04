package su.kidoz.kaillera.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO for relay status information.
 */
@Schema(description = "Relay mode status and statistics")
public record RelayStatusDTO(
        @Schema(description = "Whether relay mode is enabled", example = "true") boolean enabled,
        @Schema(description = "Whether the relay is currently running", example = "true") boolean running,
        @Schema(description = "Port the relay is listening on", example = "27887") int listenPort,
        @Schema(description = "Backend server host", example = "localhost") String backendHost,
        @Schema(description = "Backend server port", example = "27888") int backendPort,
        @Schema(description = "Current number of active connections", example = "5") int activeConnections,
        @Schema(description = "Total connections handled since startup", example = "100") int totalConnections,
        @Schema(description = "Total bytes relayed since startup", example = "1048576") long bytesRelayed,
        @Schema(description = "Number of parse errors encountered", example = "2") int parseErrors,
        @Schema(description = "Relay uptime in minutes", example = "60") long uptimeMinutes,
        @Schema(description = "Active V086 relay ports") List<V086RelayDTO> v086Relays) {

    /**
     * Creates a disabled relay status.
     */
    public static RelayStatusDTO disabled() {
        return new RelayStatusDTO(false, false, 0, "", 0, 0, 0, 0, 0, 0, List.of());
    }

    /**
     * Nested DTO for V086 relay information.
     */
    @Schema(description = "V086 protocol relay status")
    public record V086RelayDTO(@Schema(description = "Port number", example = "27889") int port,
            @Schema(description = "Active connections on this port", example = "2") int activeConnections,
            @Schema(description = "Last client message number", example = "1234") int lastClientMsgNum,
            @Schema(description = "Last server message number", example = "5678") int lastServerMsgNum) {
    }
}
