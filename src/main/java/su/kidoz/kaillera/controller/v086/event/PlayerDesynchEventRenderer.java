package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086GameEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086GameEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.GameChat_Notification;
import su.kidoz.kaillera.model.event.GameEvent;
import su.kidoz.kaillera.model.event.PlayerDesynchEvent;
import su.kidoz.util.EmuLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@V086GameEvent(eventType = PlayerDesynchEvent.class)
public final class PlayerDesynchEventRenderer implements V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(PlayerDesynchEventRenderer.class);
    private static final String DESC = PlayerDesynchEventRenderer.class.getSimpleName();

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public PlayerDesynchEventRenderer() {
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

        PlayerDesynchEvent desynchEvent = (PlayerDesynchEvent) event;

        try {
            clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(),
                    EmuLang.getString("PlayerDesynchEventRenderer.DesynchDetected"),
                    desynchEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to construct GameChat_Notification message: " + e.getMessage(), e);
        }
    }
}
