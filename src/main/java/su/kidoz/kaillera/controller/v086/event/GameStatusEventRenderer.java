package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086ServerEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086ServerEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.GameStatus;
import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.event.GameStatusChangedEvent;
import su.kidoz.kaillera.model.event.ServerEvent;

@Component
@V086ServerEvent(eventType = GameStatusChangedEvent.class)
public final class GameStatusEventRenderer implements V086ServerEventHandler {
    private static final Logger log = LoggerFactory.getLogger(GameStatusEventRenderer.class);
    private static final String DESC = "GameStatusEventRenderer";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public GameStatusEventRenderer() {
    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    @Override
    public void handleEvent(ServerEvent event, V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        GameStatusChangedEvent statusChangeEvent = (GameStatusChangedEvent) event;

        try {
            KailleraGame game = statusChangeEvent.getGame();
            clientHandler.send(new GameStatus(clientHandler.getNextMessageNumber(), game.getID(),
                    (short) 0, (byte) game.getStatus(), (byte) game.getNumPlayers(), (byte) 2));
        } catch (MessageFormatException e) {
            log.error("Failed to construct GameStatus message: " + e.getMessage(), e);
        }
    }
}
