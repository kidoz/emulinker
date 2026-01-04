package su.kidoz.kaillera.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for admin actions that return a success/failure result.
 *
 * @param success
 *            whether the action completed successfully
 * @param message
 *            optional message providing details about the result
 */
@Schema(description = "Result of an administrative action")
public record ActionResultDTO(
        @Schema(description = "Whether the action completed successfully", example = "true") boolean success,
        @Schema(description = "Message providing details about the result", example = "User Player1 has been kicked", nullable = true) String message) {

    /**
     * Creates a successful result with no message.
     */
    public static ActionResultDTO ok() {
        return new ActionResultDTO(true, null);
    }

    /**
     * Creates a successful result with a message.
     */
    public static ActionResultDTO ok(String message) {
        return new ActionResultDTO(true, message);
    }

    /**
     * Creates a failure result with a message.
     */
    public static ActionResultDTO error(String message) {
        return new ActionResultDTO(false, message);
    }
}
