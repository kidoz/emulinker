package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.GameChatException;

public class GameChatAction implements V086Action, V086GameEventHandler {
    public static final String ADMIN_COMMAND_ESCAPE_STRING = "/";

    private static final Logger log = LoggerFactory.getLogger(GameChatAction.class);
    private static final String DESC = "GameChatAction";
    private static GameChatAction singleton = new GameChatAction();

    public static GameChatAction getInstance() {
        return singleton;
    }

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);

    private GameChatAction() {

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
        if (!(message instanceof GameChat_Request))
            throw new FatalActionException("Received incorrect instance of GameChat: " + message);

        actionCount.incrementAndGet();

        GameChat_Request gameChatMessage = (GameChat_Request) message;

        try {
            clientHandler.getUser().gameChat(gameChatMessage.getMessage(),
                    gameChatMessage.getNumber());
        } catch (GameChatException e) {
            log.debug("Failed to send game chat message: " + e.getMessage());
        }

        if (((GameChat) message).getMessage().startsWith(ADMIN_COMMAND_ESCAPE_STRING)) {
            try {
                GameOwnerCommandAction.getInstance().performAction(message, clientHandler);
            } catch (FatalActionException e) {
                log.warn("GameOwner command failed, processing as chat: " + e.getMessage());
            }
        }

    }

    public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        GameChatEvent gameChatEvent = (GameChatEvent) event;

        try {
            clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(),
                    gameChatEvent.getUser().getName(), gameChatEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct GameChat_Notification message: " + e.getMessage(), e);
        }
    }
}
