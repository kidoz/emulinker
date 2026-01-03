package su.kidoz.kaillera.controller.v086.command;

import su.kidoz.kaillera.controller.v086.action.FatalActionException;
import su.kidoz.kaillera.controller.v086.action.V086Action;
import su.kidoz.kaillera.controller.v086.annotation.V086Command;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.QuitGame;
import su.kidoz.kaillera.controller.v086.protocol.QuitGame_Request;
import su.kidoz.kaillera.controller.v086.protocol.V086Message;
import su.kidoz.kaillera.model.exception.CloseGameException;
import su.kidoz.kaillera.model.exception.DropGameException;
import su.kidoz.kaillera.model.exception.QuitGameException;

/**
 * Command handler for inbound quit game requests from clients.
 *
 * <p>
 * Processes QuitGame_Request messages and delegates to the domain model.
 * Outbound event rendering is handled by {@link QuitGameEventRenderer}.
 */
@Component
@V086Command(messageId = QuitGame.ID)
public final class QuitGameCommandAction implements V086Action {
    private static final Logger log = LoggerFactory.getLogger(QuitGameCommandAction.class);
    private static final String DESC = "QuitGameCommandAction";

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
        if (!(message instanceof QuitGame_Request)) {
            throw new FatalActionException("Received incorrect instance of QuitGame: " + message);
        }

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
}
