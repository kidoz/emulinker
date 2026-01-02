package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.AllReady;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.exception.UserReadyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserReadyAction implements V086Action, V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(UserReadyAction.class);
    private static final String DESC = "UserReadyAction";

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);

    public UserReadyAction() {
    }

    public int getActionPerformedCount() {
        return actionCount.get();
    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void performAction(V086Message message, V086ClientHandler clientHandler)
            throws FatalActionException {
        actionCount.incrementAndGet();

        try {
            clientHandler.getUser().playerReady();
        } catch (UserReadyException e) {
            log.debug("Ready signal failed: " + e.getMessage());
        }
    }

    public void handleEvent(GameEvent event, V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        clientHandler.resetGameDataCache();

        try {
            clientHandler.send(new AllReady(clientHandler.getNextMessageNumber()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct AllReady message: " + e.getMessage(), e);
        }
    }
}
