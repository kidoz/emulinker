package su.kidoz.kaillera.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for kicking a user from the server.
 *
 * @param reason
 *            the reason for kicking the user (shown in disconnect message)
 */
@Schema(description = "Request to kick a user from the server")
public record KickUserRequest(
        @Schema(description = "Reason for kicking the user (optional, shown to the user)", example = "Excessive lag", nullable = true) String reason) {
}
