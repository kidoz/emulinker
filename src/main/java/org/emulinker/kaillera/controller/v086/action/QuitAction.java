package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.Quit_Notification;
import org.emulinker.kaillera.controller.v086.protocol.Quit_Request;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.ServerEvent;
import org.emulinker.kaillera.model.event.UserQuitEvent;
import org.emulinker.kaillera.model.exception.ActionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuitAction implements V086Action, V086ServerEventHandler {
    private static final Logger log = LoggerFactory.getLogger(QuitAction.class);
    private static final String DESC = "QuitAction";

    private static QuitAction singleton = new QuitAction();

    public static QuitAction getInstance() {
        return singleton;
    }

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);

    private QuitAction() {

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

    public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
            throws FatalActionException {
        if (!(message instanceof Quit_Request))
            throw new FatalActionException("Received incorrect instance of Quit: " + message);

        actionCount.incrementAndGet();

        Quit_Request quitRequest = (Quit_Request) message;

        try {
            clientHandler.getUser().quit(quitRequest.getMessage());
        } catch (ActionException e) {
            throw new FatalActionException("Failed to quit: " + e.getMessage());
        }
    }

    public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        UserQuitEvent userQuitEvent = (UserQuitEvent) event;

        try {
            KailleraUser user = userQuitEvent.getUser();
            clientHandler.send(new Quit_Notification(clientHandler.getNextMessageNumber(),
                    user.getName(), user.getID(), userQuitEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct Quit_Notification message: " + e.getMessage(), e);
        }
    }
}
