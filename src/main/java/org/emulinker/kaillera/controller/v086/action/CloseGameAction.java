package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.CloseGame;
import org.emulinker.kaillera.model.event.*;

public class CloseGameAction implements V086ServerEventHandler {
    private static final Logger log = LoggerFactory.getLogger(CloseGameAction.class);
    private static final String desc = "CloseGameAction";
    private static CloseGameAction singleton = new CloseGameAction();

    public static CloseGameAction getInstance() {
        return singleton;
    }

    private final AtomicInteger handledCount = new AtomicInteger(0);

    private CloseGameAction() {

    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return desc;
    }

    public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler) {
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
