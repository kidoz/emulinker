package su.kidoz.kaillera.admin.dto;

/**
 * DTO for user information.
 */
public record UserDTO(int id, String name, String status, String connectionType, int ping,
        String address, long connectTime) {
}
