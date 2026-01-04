package su.kidoz.kaillera.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for user event queue metrics.
 */
@Schema(description = "Event queue metrics for a user")
public record EventMetricsDTO(@Schema(description = "User ID", example = "1") int userId,
        @Schema(description = "User name", example = "Player1") String userName,
        @Schema(description = "Current events in queue", example = "50") int queueSize,
        @Schema(description = "Number of dropped events due to queue overflow", example = "0") int droppedEvents,
        @Schema(description = "Maximum queue capacity", example = "2000") int queueCapacity,
        @Schema(description = "Queue utilization percentage", example = "2.5") double queueUtilization) {

    private static final int DEFAULT_QUEUE_CAPACITY = 2000;

    /**
     * Creates event metrics from user data.
     *
     * @param userId
     *            the user's ID
     * @param userName
     *            the user's name
     * @param queueSize
     *            current queue size
     * @param droppedEvents
     *            count of dropped events
     * @return the event metrics DTO
     */
    public static EventMetricsDTO of(int userId, String userName, int queueSize,
            int droppedEvents) {
        double utilization = (double) queueSize / DEFAULT_QUEUE_CAPACITY * 100.0;
        return new EventMetricsDTO(userId, userName, queueSize, droppedEvents,
                DEFAULT_QUEUE_CAPACITY, utilization);
    }
}
