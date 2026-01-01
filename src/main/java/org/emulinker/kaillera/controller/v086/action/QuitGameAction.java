package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;

public class QuitGameAction implements V086Action, V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(QuitGameAction.class);
    private static final String desc = "QuitGameAction";
    private static QuitGameAction singleton = new QuitGameAction();

    public static QuitGameAction getInstance() {
        return singleton;
    }

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);

    private QuitGameAction() {

    }

    public int getActionPerformedCount() {
        return actionCount.get();
    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return desc;
    }

    public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
            throws FatalActionException {
        if (!(message instanceof QuitGame_Request))
            throw new FatalActionException("Received incorrect instance of QuitGame: " + message);

        actionCount.incrementAndGet();

        try {
            clientHandler.getUser().quitGame();
        } catch (DropGameException e) {
            log.debug("Failed to drop game: " + e.getMessage());
        } catch (QuitGameException e) {
            log.debug("Failed to quit game: " + e.getMessage());
        } catch (CloseGameException e) {
            log.debug("Failed to close game: " + e.getMessage());
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.error("Sleep Interrupted!", e);
        }
    }

    public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        UserQuitGameEvent userQuitEvent = (UserQuitGameEvent) event;

        try {
            KailleraUser user = userQuitEvent.getUser();
            clientHandler.send(new QuitGame_Notification(clientHandler.getNextMessageNumber(),
                    user.getName(), user.getID()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct QuitGame_Notification message: " + e.getMessage(), e);
        }
    }
}
