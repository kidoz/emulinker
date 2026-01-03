package su.kidoz.kaillera.controller.v086.command;

import su.kidoz.kaillera.controller.v086.action.FatalActionException;
import su.kidoz.kaillera.controller.v086.action.V086Action;
import su.kidoz.kaillera.controller.v086.annotation.V086Command;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.Quit;
import su.kidoz.kaillera.controller.v086.protocol.Quit_Request;
import su.kidoz.kaillera.controller.v086.protocol.V086Message;
import su.kidoz.kaillera.model.exception.ActionException;

/**
 * Command handler for inbound quit requests from clients.
 *
 * <p>
 * Processes Quit_Request messages and delegates to the domain model. Outbound
 * event rendering is handled by {@link QuitEventRenderer}.
 */
@Component
@V086Command(messageId = Quit.ID)
public final class QuitCommandAction implements V086Action {
    private static final String DESC = "QuitCommandAction";

    private final AtomicInteger actionCount = new AtomicInteger(0);

    public int getActionPerformedCount() {
        return actionCount.get();
    }

    public String toString() {
        return DESC;
    }

    @Override
    public void performAction(final V086Message message, final V086ClientHandler clientHandler)
            throws FatalActionException {
        if (!(message instanceof Quit_Request)) {
            throw new FatalActionException("Received incorrect instance of Quit: " + message);
        }

        actionCount.incrementAndGet();

        Quit_Request quitRequest = (Quit_Request) message;

        try {
            clientHandler.getUser().quit(quitRequest.getMessage());
        } catch (ActionException e) {
            throw new FatalActionException("Failed to quit: " + e.getMessage());
        }
    }
}
