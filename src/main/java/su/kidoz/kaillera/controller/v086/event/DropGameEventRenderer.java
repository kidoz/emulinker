package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086GameEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086GameEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.PlayerDrop_Notification;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.GameEvent;
import su.kidoz.kaillera.model.event.UserDroppedGameEvent;

/**
 * Event renderer for outbound player drop notifications.
 *
 * <p>
 * Renders UserDroppedGameEvent domain events into PlayerDrop_Notification
 * protocol messages. Inbound command handling is done by
 * {@link DropGameCommandAction}.
 */
@Component
@V086GameEvent(eventType = UserDroppedGameEvent.class)
public final class DropGameEventRenderer implements V086GameEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DropGameEventRenderer.class);
    private static final String DESC = "DropGameEventRenderer";

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

        UserDroppedGameEvent userDroppedEvent = (UserDroppedGameEvent) event;

        try {
            KailleraUser user = userDroppedEvent.getUser();
            int playerNumber = userDroppedEvent.getPlayerNumber();
            clientHandler.send(new PlayerDrop_Notification(clientHandler.getNextMessageNumber(),
                    user.getName(), (byte) playerNumber));
        } catch (MessageFormatException e) {
            log.error("Failed to construct PlayerDrop_Notification message: " + e.getMessage(), e);
        }
    }
}
