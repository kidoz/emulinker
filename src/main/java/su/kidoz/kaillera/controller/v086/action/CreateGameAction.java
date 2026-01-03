package su.kidoz.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.CreateGame;
import su.kidoz.kaillera.controller.v086.protocol.CreateGame_Notification;
import su.kidoz.kaillera.controller.v086.protocol.CreateGame_Request;
import su.kidoz.kaillera.controller.v086.protocol.InformationMessage;
import su.kidoz.kaillera.controller.v086.protocol.QuitGame_Notification;
import su.kidoz.kaillera.controller.v086.protocol.V086Message;
import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.GameCreatedEvent;
import su.kidoz.kaillera.model.event.ServerEvent;
import su.kidoz.kaillera.model.exception.CreateGameException;
import su.kidoz.kaillera.model.exception.FloodException;
import su.kidoz.util.EmuLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CreateGameAction implements V086Action, V086ServerEventHandler {
    private static final Logger log = LoggerFactory.getLogger(CreateGameAction.class);
    private static final String DESC = "CreateGameAction";

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);

    public CreateGameAction() {
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

    public void performAction(V086Message message, V086ClientHandler clientHandler)
            throws FatalActionException {
        if (!(message instanceof CreateGame_Request))
            throw new FatalActionException("Received incorrect instance of CreateGame: " + message);

        actionCount.incrementAndGet();

        CreateGame createGameMessage = (CreateGame) message;

        try {
            clientHandler.getUser().createGame(createGameMessage.getRomName());
        } catch (CreateGameException e) {
            log.info("Create Game Denied: " + clientHandler.getUser() + ": "
                    + createGameMessage.getRomName());

            try {
                clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                        "server",
                        EmuLang.getString("CreateGameAction.CreateGameDenied", e.getMessage())));
                clientHandler.send(new QuitGame_Notification(clientHandler.getNextMessageNumber(),
                        clientHandler.getUser().getName(), clientHandler.getUser().getID()));
            } catch (MessageFormatException e2) {
                log.error("Failed to contruct message: " + e.getMessage(), e);
            }
        } catch (FloodException e) {
            log.info("Create Game Denied: " + clientHandler.getUser() + ": "
                    + createGameMessage.getRomName());

            try {
                clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                        "server",
                        EmuLang.getString("CreateGameAction.CreateGameDeniedFloodControl")));
                clientHandler.send(new QuitGame_Notification(clientHandler.getNextMessageNumber(),
                        clientHandler.getUser().getName(), clientHandler.getUser().getID()));
            } catch (MessageFormatException e2) {
                log.error("Failed to contruct message: " + e.getMessage(), e);
            }
        }
    }

    public void handleEvent(ServerEvent event, V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        GameCreatedEvent gameCreatedEvent = (GameCreatedEvent) event;

        try {
            KailleraGame game = gameCreatedEvent.getGame();
            KailleraUser owner = game.getOwner();
            clientHandler.send(new CreateGame_Notification(clientHandler.getNextMessageNumber(),
                    owner.getName(), game.getRomName(), owner.getClientType(), game.getID(),
                    (short) 0));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct CreateGame_Notification message: " + e.getMessage(), e);
        }
    }
}
