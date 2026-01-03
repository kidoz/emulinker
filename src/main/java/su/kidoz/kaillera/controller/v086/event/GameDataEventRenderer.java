package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086GameEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086GameEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.CachedGameData;
import su.kidoz.kaillera.controller.v086.protocol.GameData;
import su.kidoz.kaillera.model.event.GameDataEvent;
import su.kidoz.kaillera.model.event.GameEvent;

/**
 * Event renderer for outbound game data notifications.
 *
 * <p>
 * Renders GameDataEvent domain events into GameData or CachedGameData protocol
 * messages. Uses caching to reduce bandwidth for repeated data. Inbound command
 * handling is done by {@link GameDataCommandAction}.
 */
@Component
@V086GameEvent(eventType = GameDataEvent.class)
public final class GameDataEventRenderer implements V086GameEventHandler {

    private static final Logger log = LoggerFactory.getLogger(GameDataEventRenderer.class);
    private static final String DESC = "GameDataEventRenderer";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    @Override
    public void handleEvent(final GameEvent event, final V086ClientHandler clientHandler) {
        byte[] data = ((GameDataEvent) event).getData();
        int key = clientHandler.getServerGameDataCache().indexOf(data);
        if (key < 0) {
            clientHandler.getServerGameDataCache().add(data);

            try {
                clientHandler.send(new GameData(clientHandler.getNextMessageNumber(), data));
            } catch (MessageFormatException e) {
                log.error("Failed to construct GameData message: " + e.getMessage(), e);
            }
        } else {
            try {
                clientHandler.send(new CachedGameData(clientHandler.getNextMessageNumber(), key));
            } catch (MessageFormatException e) {
                log.error("Failed to construct CachedGameData message: " + e.getMessage(), e);
            }
        }
    }
}
