package org.emulinker.kaillera.controller.v086.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.GameChat_Notification;
import org.emulinker.kaillera.model.event.*;

public class GameInfoAction implements V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(GameInfoAction.class);
    private static final String desc = "GameInfoAction";
    private static GameInfoAction singleton = new GameInfoAction();

    public static GameInfoAction getInstance() {
        return singleton;
    }

    private int handledCount = 0;

    private GameInfoAction() {

    }

    public int getHandledEventCount() {
        return handledCount;
    }

    public String toString() {
        return desc;
    }

    public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount++;

        GameInfoEvent infoEvent = (GameInfoEvent) event;

        try {
            clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(),
                    "Server", infoEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct GameChat_Notification message: " + e.getMessage(), e);
        }
    }
}
