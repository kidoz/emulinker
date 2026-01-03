package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086ServerEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086ServerEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.Chat_Notification;
import su.kidoz.kaillera.model.event.ChatEvent;
import su.kidoz.kaillera.model.event.ServerEvent;

/**
 * Event renderer for outbound chat notifications.
 *
 * <p>
 * Renders ChatEvent domain events into Chat_Notification protocol messages.
 * This class handles only the outbound (event) side of chat; inbound command
 * handling is done by {@link ChatCommandAction}.
 */
@Component
@V086ServerEvent(eventType = ChatEvent.class)
public final class ChatEventRenderer implements V086ServerEventHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatEventRenderer.class);
    private static final String DESC = "ChatEventRenderer";

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

        try {
            ChatEvent chatEvent = (ChatEvent) event;
            clientHandler.send(new Chat_Notification(clientHandler.getNextMessageNumber(),
                    chatEvent.getUser().getName(), chatEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to construct Chat_Notification message: " + e.getMessage(), e);
        }
    }
}
