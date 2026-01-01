package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.GameKick;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.exception.GameKickException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameKickAction implements V086Action {
    private static final Logger log = LoggerFactory.getLogger(GameKickAction.class);
    private static final String DESC = "GameKickAction";
    private static GameKickAction singleton = new GameKickAction();

    public static GameKickAction getInstance() {
        return singleton;
    }

    private final AtomicInteger actionCount = new AtomicInteger(0);

    private GameKickAction() {

    }

    public int getActionPerformedCount() {
        return actionCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
            throws FatalActionException {
        actionCount.incrementAndGet();

        GameKick kickRequest = (GameKick) message;

        try {
            clientHandler.getUser().gameKick(kickRequest.getUserID());
        } catch (GameKickException e) {
            log.debug("Failed to kick: " + e.getMessage());
        }
    }
}
