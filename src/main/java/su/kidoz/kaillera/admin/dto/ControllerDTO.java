package su.kidoz.kaillera.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO for controller information.
 */
@Schema(description = "Protocol controller information")
public record ControllerDTO(
        @Schema(description = "Protocol version", example = "0.86") String version,
        @Schema(description = "Buffer size in bytes", example = "2048") int bufferSize,
        @Schema(description = "Number of connected clients", example = "10") int numClients,
        @Schema(description = "Supported client types", example = "[\"0.83\"]") List<String> clientTypes) {
}
