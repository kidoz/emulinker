package su.kidoz.kaillera.controller.v086.command;

import su.kidoz.kaillera.controller.v086.action.FatalActionException;
import su.kidoz.kaillera.controller.v086.action.V086Action;
import su.kidoz.kaillera.controller.v086.annotation.V086Command;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.GameChat_Notification;
import su.kidoz.kaillera.controller.v086.protocol.StartGame;
import su.kidoz.kaillera.controller.v086.protocol.StartGame_Request;
import su.kidoz.kaillera.controller.v086.protocol.V086Message;
import su.kidoz.kaillera.model.exception.StartGameException;

/**
 * Command handler for inbound start game requests from clients.
 *
 * <p>
 * Processes StartGame_Request messages and delegates to the domain model.
 * Outbound event rendering is handled by {@link StartGameEventRenderer}.
 */
@Component
@V086Command(messageId = StartGame.ID)
public final class StartGameCommandAction implements V086Action {
    private static final Logger log = LoggerFactory.getLogger(StartGameCommandAction.class);
    private static final String DESC = "StartGameCommandAction";

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
        if (!(message instanceof StartGame_Request)) {
            throw new FatalActionException("Received incorrect instance of StartGame: " + message);
        }

        actionCount.incrementAndGet();

        try {
            clientHandler.getUser().startGame();
        } catch (StartGameException e) {
            log.debug("Failed to start game: " + e.getMessage());

            try {
                clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(),
                        "Error", e.getMessage()));
            } catch (MessageFormatException ex) {
                log.error("Failed to construct GameChat_Notification message: " + e.getMessage(),
                        e);
            }
        }
    }
}
