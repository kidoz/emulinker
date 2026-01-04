package su.kidoz.kaillera.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for user information.
 */
@Schema(description = "Connected user information")
public record UserDTO(@Schema(description = "Unique user ID", example = "1") int id,
        @Schema(description = "User display name", example = "Player1") String name,
        @Schema(description = "Current user status", example = "Playing", allowableValues = {
                "Idle", "Playing", "Connecting"}) String status,
        @Schema(description = "Connection type", example = "LAN", allowableValues = {"LAN",
                "Excellent", "Good", "Average", "Low", "Bad"}) String connectionType,
        @Schema(description = "User ping in milliseconds", example = "50") int ping,
        @Schema(description = "User IP address and port", example = "192.168.1.100:27889") String address,
        @Schema(description = "Connection timestamp in milliseconds since epoch", example = "1704067200000") long connectTime){
}
