package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086GameEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086GameEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.GameChat_Notification;
import su.kidoz.kaillera.model.event.GameChatEvent;
import su.kidoz.kaillera.model.event.GameEvent;

/**
 * Event renderer for outbound game chat notifications.
 *
 * <p>
 * Renders GameChatEvent domain events into GameChat_Notification protocol
 * messages. Inbound command handling is done by {@link GameChatCommandAction}.
 */
@Component
@V086GameEvent(eventType = GameChatEvent.class)
public final class GameChatEventRenderer implements V086GameEventHandler {

    private static final Logger log = LoggerFactory.getLogger(GameChatEventRenderer.class);
    private static final String DESC = "GameChatEventRenderer";

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

        GameChatEvent gameChatEvent = (GameChatEvent) event;

        try {
            clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(),
                    gameChatEvent.getUser().getName(), gameChatEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to construct GameChat_Notification message: " + e.getMessage(), e);
        }
    }
}
