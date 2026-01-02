package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.GameChat_Notification;
import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.event.PlayerDesynchEvent;
import org.emulinker.util.EmuLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlayerDesynchAction implements V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(PlayerDesynchAction.class);
    private static final String DESC = PlayerDesynchAction.class.getSimpleName();

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public PlayerDesynchAction() {
    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void handleEvent(GameEvent event, V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        PlayerDesynchEvent desynchEvent = (PlayerDesynchEvent) event;

        try {
            clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(),
                    EmuLang.getString("PlayerDesynchAction.DesynchDetected"),
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
