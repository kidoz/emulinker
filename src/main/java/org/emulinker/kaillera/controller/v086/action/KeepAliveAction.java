package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;

public class KeepAliveAction implements V086Action {
    // private static final Logger log =
    // LoggerFactory.getLogger(KeepAliveAction.class);
    private static final String DESC = "KeepAliveAction";
    private static KeepAliveAction singleton = new KeepAliveAction();

    public static KeepAliveAction getInstance() {
        return singleton;
    }

    private final AtomicInteger actionCount = new AtomicInteger(0);

    private KeepAliveAction() {

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
        clientHandler.getUser().updateLastKeepAlive();
    }
}
