package su.kidoz.kaillera.model;

/**
 * Represents the progression of login notification states.
 *
 * <p>
 * During login, multiple events must be sent to the client in sequence. This
 * enum tracks the current state to ensure proper ordering without using
 * arbitrary Thread.sleep() delays.
 *
 * <p>
 * State progression:
 *
 * <pre>
 * CONNECTED → USER_JOINED → MESSAGES_SENT → ADMIN_INFO → COMPLETE
 * </pre>
 */
public enum LoginNotificationState {

    /**
     * Initial state after ConnectedEvent has been sent to the user.
     */
    CONNECTED,

    /**
     * UserJoinedEvent has been broadcast to all users.
     */
    USER_JOINED,

    /**
     * All login messages have been sent to the user.
     */
    MESSAGES_SENT,

    /**
     * Admin-specific info has been sent (if applicable).
     */
    ADMIN_INFO,

    /**
     * Login notification sequence is complete.
     */
    COMPLETE;

    /**
     * Gets the next state in the login notification sequence.
     *
     * @return the next state, or COMPLETE if already complete
     */
    public LoginNotificationState next() {
        return switch (this) {
            case CONNECTED -> USER_JOINED;
            case USER_JOINED -> MESSAGES_SENT;
            case MESSAGES_SENT -> ADMIN_INFO;
            case ADMIN_INFO, COMPLETE -> COMPLETE;
        };
    }

    /**
     * Checks if the login notification sequence is complete.
     *
     * @return true if complete
     */
    public boolean isComplete() {
        return this == COMPLETE;
    }
}
