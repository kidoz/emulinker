package su.kidoz.kaillera.admin.dto;

/**
 * Request DTO for kicking a user from the server.
 *
 * @param reason
 *            the reason for kicking the user (shown in disconnect message)
 */
public record KickUserRequest(String reason) {
}
