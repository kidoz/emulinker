package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086ServerEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086ServerEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.access.AccessManager;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.InformationMessage;
import su.kidoz.kaillera.controller.v086.protocol.UserJoined;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.ServerEvent;
import su.kidoz.kaillera.model.event.UserJoinedEvent;

/**
 * Event renderer for outbound user joined notifications.
 *
 * <p>
 * Renders UserJoinedEvent domain events into UserJoined protocol messages. This
 * class handles only the outbound (event) side of login; inbound command
 * handling is done by {@link LoginCommandAction}.
 */
@Component
@V086ServerEvent(eventType = UserJoinedEvent.class)
public final class LoginEventRenderer implements V086ServerEventHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginEventRenderer.class);
    private static final String DESC = "LoginEventRenderer";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    @Override
    public void handleEvent(final ServerEvent event, final V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        UserJoinedEvent userJoinedEvent = (UserJoinedEvent) event;

        try {
            KailleraUser user = userJoinedEvent.getUser();
            clientHandler.send(new UserJoined(clientHandler.getNextMessageNumber(), user.getName(),
                    user.getID(), user.getPing(), (byte) user.getConnectionType()));

            KailleraUser thisUser = clientHandler.getUser();
            if (thisUser.isEmuLinkerClient()
                    && thisUser.getAccess() == AccessManager.ACCESS_ADMIN) {
                if (!user.equals(thisUser)) {
                    clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                            "server",
                            ":USERINFO=" + user.getID() + ","
                                    + user.getConnectSocketAddress().getAddress().getHostAddress()
                                    + "," + user.getAccessStr()));
                }
            }
        } catch (MessageFormatException e) {
            log.error("Failed to construct UserJoined_Notification message: " + e.getMessage(), e);
        }
    }
}
