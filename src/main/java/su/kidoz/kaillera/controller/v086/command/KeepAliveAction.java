package su.kidoz.kaillera.controller.v086.command;

import su.kidoz.kaillera.controller.v086.action.FatalActionException;
import su.kidoz.kaillera.controller.v086.action.V086Action;
import su.kidoz.kaillera.controller.v086.annotation.V086Command;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.KeepAlive;
import su.kidoz.kaillera.controller.v086.protocol.V086Message;

@Component
@V086Command(messageId = KeepAlive.ID)
public final class KeepAliveAction implements V086Action {
    private static final String DESC = "KeepAliveAction";

    private final AtomicInteger actionCount = new AtomicInteger(0);

    public KeepAliveAction() {
    }

    public int getActionPerformedCount() {
        return actionCount.get();
    }

    public String toString() {
        return DESC;
    }

    @Override
    public void performAction(V086Message message, V086ClientHandler clientHandler)
            throws FatalActionException {
        actionCount.incrementAndGet();
        clientHandler.getUser().updateLastKeepAlive();
    }
}
