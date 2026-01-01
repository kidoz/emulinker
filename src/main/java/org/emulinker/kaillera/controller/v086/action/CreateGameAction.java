package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.CreateGame;
import org.emulinker.kaillera.controller.v086.protocol.CreateGame_Notification;
import org.emulinker.kaillera.controller.v086.protocol.CreateGame_Request;
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage;
import org.emulinker.kaillera.controller.v086.protocol.QuitGame_Notification;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.GameCreatedEvent;
import org.emulinker.kaillera.model.event.ServerEvent;
import org.emulinker.kaillera.model.exception.CreateGameException;
import org.emulinker.kaillera.model.exception.FloodException;
import org.emulinker.util.EmuLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateGameAction implements V086Action, V086ServerEventHandler {
    private static final Logger log = LoggerFactory.getLogger(CreateGameAction.class);
    private static final String DESC = "CreateGameAction";
    private static CreateGameAction singleton = new CreateGameAction();

    public static CreateGameAction getInstance() {
        return singleton;
    }

    private final AtomicInteger actionCount = new AtomicInteger(0);

    private final AtomicInteger handledCount = new AtomicInteger(0);

    private CreateGameAction() {

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

    public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
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

    public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler) {
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
