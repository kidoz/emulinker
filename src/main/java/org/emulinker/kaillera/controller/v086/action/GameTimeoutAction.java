package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.event.GameTimeoutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameTimeoutAction implements V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(GameTimeoutAction.class);
    private static final String DESC = "GameTimeoutAction";
    private static GameTimeoutAction singleton = new GameTimeoutAction();

    public static GameTimeoutAction getInstance() {
        return singleton;
    }

    private final AtomicInteger handledCount = new AtomicInteger(0);

    private GameTimeoutAction() {

    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

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
