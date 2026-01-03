package su.kidoz.kaillera.controller.v086.action;

import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.V086Message;

public interface V086Action {
    String toString();

    void performAction(V086Message message, V086ClientHandler clientHandler)
            throws FatalActionException;

    int getActionPerformedCount();
}
