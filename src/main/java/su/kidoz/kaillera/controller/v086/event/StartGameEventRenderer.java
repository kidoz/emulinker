package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086GameEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086GameEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.StartGame_Notification;
import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.event.GameEvent;
import su.kidoz.kaillera.model.event.GameStartedEvent;

/**
 * Event renderer for outbound game started notifications.
 *
 * <p>
 * Renders GameStartedEvent domain events into StartGame_Notification protocol
 * messages. Inbound command handling is done by {@link StartGameCommandAction}.
 */
@Component
@V086GameEvent(eventType = GameStartedEvent.class)
public final class StartGameEventRenderer implements V086GameEventHandler {

    private static final Logger log = LoggerFactory.getLogger(StartGameEventRenderer.class);
    private static final String DESC = "StartGameEventRenderer";

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

        GameStartedEvent gameStartedEvent = (GameStartedEvent) event;

        try {
            KailleraGame game = gameStartedEvent.getGame();
            int playerNumber = game.getPlayerNumber(clientHandler.getUser());
            clientHandler.send(new StartGame_Notification(clientHandler.getNextMessageNumber(),
                    (short) 2, (byte) playerNumber, (byte) game.getNumPlayers()));
        } catch (MessageFormatException e) {
            log.error("Failed to construct StartGame_Notification message: " + e.getMessage(), e);
        }
    }
}
