package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086GameEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086GameEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.AllReady;
import su.kidoz.kaillera.model.event.AllReadyEvent;
import su.kidoz.kaillera.model.event.GameEvent;

/**
 * Event renderer for outbound all ready notifications.
 *
 * <p>
 * Renders AllReadyEvent domain events into AllReady protocol messages. Inbound
 * command handling is done by {@link UserReadyCommandAction}.
 */
@Component
@V086GameEvent(eventType = AllReadyEvent.class)
public final class UserReadyEventRenderer implements V086GameEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UserReadyEventRenderer.class);
    private static final String DESC = "UserReadyEventRenderer";

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

        clientHandler.resetGameDataCache();

        try {
            clientHandler.send(new AllReady(clientHandler.getNextMessageNumber()));
        } catch (MessageFormatException e) {
            log.error("Failed to construct AllReady message: " + e.getMessage(), e);
        }
    }
}
