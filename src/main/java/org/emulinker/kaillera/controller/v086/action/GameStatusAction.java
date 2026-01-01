package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.GameStatus;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.event.GameStatusChangedEvent;
import org.emulinker.kaillera.model.event.ServerEvent;

public class GameStatusAction implements V086ServerEventHandler {
    private static final Logger log = LoggerFactory.getLogger(GameStatusAction.class);
    private static final String DESC = "GameStatusAction";
    private static GameStatusAction singleton = new GameStatusAction();

    public static GameStatusAction getInstance() {
        return singleton;
    }

    private final AtomicInteger handledCount = new AtomicInteger(0);

    private GameStatusAction() {

    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        GameStatusChangedEvent statusChangeEvent = (GameStatusChangedEvent) event;

        try {
            KailleraGame game = statusChangeEvent.getGame();
            clientHandler.send(new GameStatus(clientHandler.getNextMessageNumber(), game.getID(),
                    (short) 0, (byte) game.getStatus(), (byte) game.getNumPlayers(), (byte) 2));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct CreateGame_Notification message: " + e.getMessage(), e);
        }
    }
}
