package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.QuitGame_Notification;
import org.emulinker.kaillera.controller.v086.protocol.QuitGame_Request;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.event.UserQuitGameEvent;
import org.emulinker.kaillera.model.exception.CloseGameException;
import org.emulinker.kaillera.model.exception.DropGameException;
import org.emulinker.kaillera.model.exception.QuitGameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QuitGameAction implements V086Action, V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(QuitGameAction.class);
    private static final String DESC = "QuitGameAction";

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);

    public QuitGameAction() {
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

    public void handleEvent(GameEvent event, V086ClientHandler clientHandler) {
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
