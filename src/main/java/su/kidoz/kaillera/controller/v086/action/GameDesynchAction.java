package su.kidoz.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.GameChat_Notification;
import su.kidoz.kaillera.model.event.GameDesynchEvent;
import su.kidoz.kaillera.model.event.GameEvent;
import su.kidoz.util.EmuLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameDesynchAction implements V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(GameDesynchAction.class);
    private static final String DESC = "GameDesynchAction";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public GameDesynchAction() {
    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void handleEvent(GameEvent event, V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        GameDesynchEvent desynchEvent = (GameDesynchEvent) event;

        try {
            clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(),
                    EmuLang.getString("GameDesynchAction.DesynchDetected"),
                    desynchEvent.getMessage()));
            // if (clientHandler.getUser().getStatus() == KailleraUser.STATUS_PLAYING)
            // clientHandler.getUser().dropGame();
        } catch (MessageFormatException e) {
            log.error("Failed to contruct GameChat_Notification message: " + e.getMessage(), e);
        }
        // catch (DropGameException e)
        // {
        // log.error("Failed to drop game during desynch: " + e.getMessage(), e);
        // }
    }
}
