package su.kidoz.kaillera.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for game information.
 */
@Schema(description = "Active game information")
public record GameDTO(@Schema(description = "Unique game ID", example = "1") int id,
        @Schema(description = "ROM/game name", example = "Street Fighter II") String rom,
        @Schema(description = "Game owner's name", example = "Player1") String owner,
        @Schema(description = "Current game status", example = "Playing", allowableValues = {
                "Waiting", "Playing", "Syncing"}) String status,
        @Schema(description = "Number of players in game", example = "2") int players){
}
