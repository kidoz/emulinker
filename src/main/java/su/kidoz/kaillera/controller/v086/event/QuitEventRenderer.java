package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086ServerEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086ServerEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.Quit_Notification;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.ServerEvent;
import su.kidoz.kaillera.model.event.UserQuitEvent;

/**
 * Event renderer for outbound user quit notifications.
 *
 * <p>
 * Renders UserQuitEvent domain events into Quit_Notification protocol messages.
 * Inbound command handling is done by {@link QuitCommandAction}.
 */
@Component
@V086ServerEvent(eventType = UserQuitEvent.class)
public final class QuitEventRenderer implements V086ServerEventHandler {

    private static final Logger log = LoggerFactory.getLogger(QuitEventRenderer.class);
    private static final String DESC = "QuitEventRenderer";

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

        UserQuitEvent userQuitEvent = (UserQuitEvent) event;

        try {
            KailleraUser user = userQuitEvent.getUser();
            clientHandler.send(new Quit_Notification(clientHandler.getNextMessageNumber(),
                    user.getName(), user.getID(), userQuitEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to construct Quit_Notification message: " + e.getMessage(), e);
        }
    }
}
