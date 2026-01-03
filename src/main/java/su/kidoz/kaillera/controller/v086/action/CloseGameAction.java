package su.kidoz.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.CloseGame;
import su.kidoz.kaillera.model.event.GameClosedEvent;
import su.kidoz.kaillera.model.event.ServerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CloseGameAction implements V086ServerEventHandler {
    private static final Logger log = LoggerFactory.getLogger(CloseGameAction.class);
    private static final String DESC = "CloseGameAction";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public CloseGameAction() {
    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void handleEvent(ServerEvent event, V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        GameClosedEvent gameClosedEvent = (GameClosedEvent) event;

        try {
            clientHandler.send(new CloseGame(clientHandler.getNextMessageNumber(),
                    gameClosedEvent.getGame().getID(), (short) 0));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct CloseGame_Notification message: " + e.getMessage(), e);
        }
    }
}
