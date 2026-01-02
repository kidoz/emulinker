package org.emulinker.kaillera.model.event;

import java.util.List;

import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;

import su.kidoz.kaillera.model.LoginNotificationState;

/**
 * Event that drives the login notification state machine.
 *
 * <p>
 * This event replaces the Thread.sleep() delays in the login notification flow.
 * Instead of sleeping, we advance through states by queueing the next
 * LoginProgressEvent after each step completes.
 *
 * <p>
 * The event carries all necessary context to continue the notification
 * sequence:
 * <ul>
 * <li>Current state in the notification sequence</li>
 * <li>Login messages to send</li>
 * <li>User's access level for admin-specific messages</li>
 * </ul>
 */
public final class LoginProgressEvent implements UserEvent {

    private final KailleraServer server;
    private final KailleraUser user;
    private final LoginNotificationState state;
    private final List<String> loginMessages;
    private final int accessLevel;

    /**
     * Creates a new login progress event.
     *
     * @param server
     *            the server instance
     * @param user
     *            the user going through login
     * @param state
     *            current state in the login notification sequence
     * @param loginMessages
     *            messages to send during the MESSAGES_SENT state
     * @param accessLevel
     *            user's access level for admin checks
     */
    public LoginProgressEvent(KailleraServer server, KailleraUser user,
            LoginNotificationState state, List<String> loginMessages, int accessLevel) {
        this.server = server;
        this.user = user;
        this.state = state;
        this.loginMessages = loginMessages;
        this.accessLevel = accessLevel;
    }

    /**
     * Gets the server instance.
     */
    public KailleraServer getServer() {
        return server;
    }

    @Override
    public KailleraUser getUser() {
        return user;
    }

    /**
     * Gets the current state in the login notification sequence.
     */
    public LoginNotificationState getState() {
        return state;
    }

    /**
     * Gets the login messages to send.
     */
    public List<String> getLoginMessages() {
        return loginMessages;
    }

    /**
     * Gets the user's access level.
     */
    public int getAccessLevel() {
        return accessLevel;
    }

    /**
     * Creates the next event in the login notification sequence.
     *
     * @return a new event with the next state, or null if complete
     */
    public LoginProgressEvent nextEvent() {
        LoginNotificationState nextState = state.next();
        if (nextState.isComplete()) {
            return null;
        }
        return new LoginProgressEvent(server, user, nextState, loginMessages, accessLevel);
    }

    @Override
    public String toString() {
        return "LoginProgressEvent[user=" + user.getName() + ", state=" + state + "]";
    }
}
