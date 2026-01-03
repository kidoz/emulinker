package su.kidoz.kaillera.controller.v086.command;

import su.kidoz.kaillera.controller.v086.action.FatalActionException;
import su.kidoz.kaillera.controller.v086.action.V086Action;
import su.kidoz.kaillera.controller.v086.annotation.V086Command;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.PlayerDrop;
import su.kidoz.kaillera.controller.v086.protocol.PlayerDrop_Request;
import su.kidoz.kaillera.controller.v086.protocol.V086Message;
import su.kidoz.kaillera.model.exception.DropGameException;

/**
 * Command handler for inbound player drop requests from clients.
 *
 * <p>
 * Processes PlayerDrop_Request messages and delegates to the domain model.
 * Outbound event rendering is handled by {@link DropGameEventRenderer}.
 */
@Component
@V086Command(messageId = PlayerDrop.ID)
public final class DropGameCommandAction implements V086Action {
    private static final Logger log = LoggerFactory.getLogger(DropGameCommandAction.class);
    private static final String DESC = "DropGameCommandAction";

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
        if (!(message instanceof PlayerDrop_Request))
            throw new FatalActionException("Received incorrect instance of PlayerDrop: " + message);

        actionCount.incrementAndGet();

        try {
            clientHandler.getUser().dropGame();
        } catch (DropGameException e) {
            log.debug("Failed to drop game: " + e.getMessage());
        }
    }
}
