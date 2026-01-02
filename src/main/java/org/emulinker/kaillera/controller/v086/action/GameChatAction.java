package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.GameChat;
import org.emulinker.kaillera.controller.v086.protocol.GameChat_Notification;
import org.emulinker.kaillera.controller.v086.protocol.GameChat_Request;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.event.GameChatEvent;
import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.exception.GameChatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameChatAction implements V086Action, V086GameEventHandler {
    public static final String ADMIN_COMMAND_ESCAPE_STRING = "/";

    private static final Logger log = LoggerFactory.getLogger(GameChatAction.class);
    private static final String DESC = "GameChatAction";

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);
    private final GameOwnerCommandAction gameOwnerCommandAction;

    public GameChatAction(GameOwnerCommandAction gameOwnerCommandAction) {
        this.gameOwnerCommandAction = gameOwnerCommandAction;
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
                gameOwnerCommandAction.performAction(message, clientHandler);
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
