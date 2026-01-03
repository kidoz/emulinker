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
import su.kidoz.kaillera.controller.v086.protocol.JoinGame;
import su.kidoz.kaillera.controller.v086.protocol.InformationMessage;
import su.kidoz.kaillera.controller.v086.protocol.JoinGame_Request;
import su.kidoz.kaillera.controller.v086.protocol.QuitGame_Notification;
import su.kidoz.kaillera.controller.v086.protocol.V086Message;
import su.kidoz.kaillera.model.exception.JoinGameException;
import su.kidoz.util.EmuLang;

/**
 * Command handler for inbound join game requests from clients.
 *
 * <p>
 * Processes JoinGame_Request messages and delegates to the domain model.
 * Outbound event rendering is handled by {@link JoinGameEventRenderer}.
 */
@Component
@V086Command(messageId = JoinGame.ID)
public final class JoinGameCommandAction implements V086Action {
    private static final Logger log = LoggerFactory.getLogger(JoinGameCommandAction.class);
    private static final String DESC = "JoinGameCommandAction";

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
        if (!(message instanceof JoinGame_Request)) {
            throw new FatalActionException("Received incorrect instance of JoinGame: " + message);
        }

        actionCount.incrementAndGet();

        JoinGame_Request joinGameRequest = (JoinGame_Request) message;

        try {
            clientHandler.getUser().joinGame(joinGameRequest.getGameID());
        } catch (JoinGameException e) {
            try {
                clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                        "server",
                        EmuLang.getString("JoinGameAction.JoinGameDenied") + e.getMessage()));
                clientHandler.send(new QuitGame_Notification(clientHandler.getNextMessageNumber(),
                        clientHandler.getUser().getName(), clientHandler.getUser().getID()));
            } catch (MessageFormatException e2) {
                log.error("Failed to construct new Message", e);
            }
        }
    }
}
