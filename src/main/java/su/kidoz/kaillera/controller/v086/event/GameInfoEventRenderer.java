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
import su.kidoz.kaillera.model.event.GameEvent;
import su.kidoz.kaillera.model.event.GameInfoEvent;

@Component
@V086GameEvent(eventType = GameInfoEvent.class)
public final class GameInfoEventRenderer implements V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(GameInfoEventRenderer.class);
    private static final String DESC = "GameInfoEventRenderer";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public GameInfoEventRenderer() {
    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    @Override
    public void handleEvent(GameEvent event, V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        GameInfoEvent infoEvent = (GameInfoEvent) event;

        try {
            clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(),
                    "Server", infoEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to construct GameChat_Notification message: " + e.getMessage(), e);
        }
    }
}
