package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.GameChat_Notification;
import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.event.GameInfoEvent;

public final class GameInfoAction implements V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(GameInfoAction.class);
    private static final String DESC = "GameInfoAction";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public GameInfoAction() {
    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void handleEvent(GameEvent event, V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        GameInfoEvent infoEvent = (GameInfoEvent) event;

        try {
            clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(),
                    "Server", infoEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct GameChat_Notification message: " + e.getMessage(), e);
        }
    }
}
