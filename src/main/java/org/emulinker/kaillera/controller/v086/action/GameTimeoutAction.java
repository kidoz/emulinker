package org.emulinker.kaillera.controller.v086.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.*;

public class GameTimeoutAction implements V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(GameTimeoutAction.class);
    private static final String desc = "GameTimeoutAction";
    private static GameTimeoutAction singleton = new GameTimeoutAction();

    public static GameTimeoutAction getInstance() {
        return singleton;
    }

    private int handledCount = 0;

    private GameTimeoutAction() {

    }

    public int getHandledEventCount() {
        return handledCount;
    }

    public String toString() {
        return desc;
    }

    public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount++;

        GameTimeoutEvent timeoutEvent = (GameTimeoutEvent) event;
        KailleraUser player = timeoutEvent.getUser();
        KailleraUser user = clientHandler.getUser();

        if (player.equals(user)) {
            log.debug(user + " received timeout event " + timeoutEvent.getTimeoutNumber() + " for "
                    + timeoutEvent.getGame() + ": resending messages...");
            clientHandler.resend(timeoutEvent.getTimeoutNumber());
        } else {
            log.debug(user + " received timeout event " + timeoutEvent.getTimeoutNumber() + " from "
                    + player + " for " + timeoutEvent.getGame());
        }
    }
}
