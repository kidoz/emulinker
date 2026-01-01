package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.StartGameException;

public class StartGameAction implements V086Action, V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(StartGameAction.class);
    private static final String DESC = "StartGameAction";
    private static StartGameAction singleton = new StartGameAction();

    public static StartGameAction getInstance() {
        return singleton;
    }

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);

    private StartGameAction() {

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
        if (!(message instanceof StartGame_Request))
            throw new FatalActionException("Received incorrect instance of StartGame: " + message);

        actionCount.incrementAndGet();

        try {
            clientHandler.getUser().startGame();
        } catch (StartGameException e) {
            log.debug("Failed to start game: " + e.getMessage());

            try {
                clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(),
                        "Error", e.getMessage()));
            } catch (MessageFormatException ex) {
                log.error("Failed to contruct GameChat_Notification message: " + e.getMessage(), e);
            }
        }
    }

    public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        GameStartedEvent gameStartedEvent = (GameStartedEvent) event;

        try {
            KailleraGame game = gameStartedEvent.getGame();
            int playerNumber = game.getPlayerNumber(clientHandler.getUser());
            clientHandler.send(new StartGame_Notification(clientHandler.getNextMessageNumber(),
                    (short) 2, (byte) playerNumber, (byte) game.getNumPlayers()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct StartGame_Notification message: " + e.getMessage(), e);
        }
    }
}
