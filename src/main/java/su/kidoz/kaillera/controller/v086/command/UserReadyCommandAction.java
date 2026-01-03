package su.kidoz.kaillera.controller.v086.command;

import su.kidoz.kaillera.controller.v086.action.FatalActionException;
import su.kidoz.kaillera.controller.v086.action.V086Action;
import su.kidoz.kaillera.controller.v086.annotation.V086Command;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.AllReady;
import su.kidoz.kaillera.controller.v086.protocol.V086Message;
import su.kidoz.kaillera.model.exception.UserReadyException;

/**
 * Command handler for inbound player ready signals from clients.
 *
 * <p>
 * Processes AllReady messages and delegates to the domain model. Outbound event
 * rendering is handled by {@link UserReadyEventRenderer}.
 */
@Component
@V086Command(messageId = AllReady.ID)
public final class UserReadyCommandAction implements V086Action {
    private static final Logger log = LoggerFactory.getLogger(UserReadyCommandAction.class);
    private static final String DESC = "UserReadyCommandAction";

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
        actionCount.incrementAndGet();

        try {
            clientHandler.getUser().playerReady();
        } catch (UserReadyException e) {
            log.debug("Ready signal failed: " + e.getMessage());
        }
    }
}
