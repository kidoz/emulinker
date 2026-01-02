package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.CachedGameData;
import org.emulinker.kaillera.controller.v086.protocol.GameData;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.event.GameDataEvent;
import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.exception.GameDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameDataAction implements V086Action, V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(GameDataAction.class);
    private static final String DESC = "GameDataAction";

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);

    public GameDataAction() {
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
        try {
            byte[] data = ((GameData) message).getGameData();
            clientHandler.getClientGameDataCache().add(data);
            clientHandler.getUser().addGameData(data);
        } catch (GameDataException e) {
            log.debug("Game data error: " + e.getMessage());

            if (e.hasResponse()) {
                try {
                    clientHandler.send(
                            new GameData(clientHandler.getNextMessageNumber(), e.getResponse()));
                } catch (MessageFormatException e2) {
                    log.error("Failed to contruct GameData message: " + e2.getMessage(), e2);
                }
            }
        }
    }

    public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
        byte[] data = ((GameDataEvent) event).getData();
        int key = clientHandler.getServerGameDataCache().indexOf(data);
        if (key < 0) {
            clientHandler.getServerGameDataCache().add(data);

            try {
                clientHandler.send(new GameData(clientHandler.getNextMessageNumber(), data));
            } catch (MessageFormatException e) {
                log.error("Failed to contruct GameData message: " + e.getMessage(), e);
            }
        } else {
            try {
                clientHandler.send(new CachedGameData(clientHandler.getNextMessageNumber(), key));
            } catch (MessageFormatException e) {
                log.error("Failed to contruct CachedGameData message: " + e.getMessage(), e);
            }
        }
    }
}
