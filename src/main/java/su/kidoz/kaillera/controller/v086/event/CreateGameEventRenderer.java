package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086ServerEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086ServerEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.CreateGame_Notification;
import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.GameCreatedEvent;
import su.kidoz.kaillera.model.event.ServerEvent;

/**
 * Event renderer for outbound game created notifications.
 *
 * <p>
 * Renders GameCreatedEvent domain events into CreateGame_Notification protocol
 * messages. Inbound command handling is done by
 * {@link CreateGameCommandAction}.
 */
@Component
@V086ServerEvent(eventType = GameCreatedEvent.class)
public final class CreateGameEventRenderer implements V086ServerEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CreateGameEventRenderer.class);
    private static final String DESC = "CreateGameEventRenderer";

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

        GameCreatedEvent gameCreatedEvent = (GameCreatedEvent) event;

        try {
            KailleraGame game = gameCreatedEvent.getGame();
            KailleraUser owner = game.getOwner();
            clientHandler.send(new CreateGame_Notification(clientHandler.getNextMessageNumber(),
                    owner.getName(), game.getRomName(), owner.getClientType(), game.getID(),
                    (short) 0));
        } catch (MessageFormatException e) {
            log.error("Failed to construct CreateGame_Notification message: " + e.getMessage(), e);
        }
    }
}
