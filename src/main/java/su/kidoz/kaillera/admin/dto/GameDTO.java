package su.kidoz.kaillera.admin.dto;

/**
 * DTO for game information.
 */
public record GameDTO(int id, String rom, String owner, String status, int players) {
}
