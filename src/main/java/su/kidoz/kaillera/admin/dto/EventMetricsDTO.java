package su.kidoz.kaillera.admin.dto;

/**
 * DTO for user event queue metrics.
 */
public record EventMetricsDTO(int userId, String userName, int queueSize, int droppedEvents,
        int queueCapacity, double queueUtilization) {

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
