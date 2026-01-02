package org.emulinker.kaillera.admin.dto;

/**
 * Response DTO for admin actions that return a success/failure result.
 *
 * @param success
 *            whether the action completed successfully
 * @param message
 *            optional message providing details about the result
 */
public record ActionResultDTO(boolean success, String message) {

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
