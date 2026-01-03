package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086GameEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086GameEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.QuitGame_Notification;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.GameEvent;
import su.kidoz.kaillera.model.event.UserQuitGameEvent;

/**
 * Event renderer for outbound user quit game notifications.
 *
 * <p>
 * Renders UserQuitGameEvent domain events into QuitGame_Notification protocol
 * messages. Inbound command handling is done by {@link QuitGameCommandAction}.
 */
@Component
@V086GameEvent(eventType = UserQuitGameEvent.class)
public final class QuitGameEventRenderer implements V086GameEventHandler {

    private static final Logger log = LoggerFactory.getLogger(QuitGameEventRenderer.class);
    private static final String DESC = "QuitGameEventRenderer";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    @Override
    public void handleEvent(final GameEvent event, final V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        UserQuitGameEvent userQuitEvent = (UserQuitGameEvent) event;

        try {
            KailleraUser user = userQuitEvent.getUser();
            clientHandler.send(new QuitGame_Notification(clientHandler.getNextMessageNumber(),
                    user.getName(), user.getID()));
        } catch (MessageFormatException e) {
            log.error("Failed to construct QuitGame_Notification message: " + e.getMessage(), e);
        }
    }
}
