package su.kidoz.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.Chat;
import su.kidoz.kaillera.controller.v086.protocol.Chat_Notification;
import su.kidoz.kaillera.controller.v086.protocol.Chat_Request;
import su.kidoz.kaillera.controller.v086.protocol.InformationMessage;
import su.kidoz.kaillera.controller.v086.protocol.V086Message;
import su.kidoz.kaillera.model.event.ChatEvent;
import su.kidoz.kaillera.model.event.ServerEvent;
import su.kidoz.kaillera.model.exception.ActionException;
import su.kidoz.util.EmuLang;

public final class ChatAction implements V086Action, V086ServerEventHandler {
    public static final String ADMIN_COMMAND_ESCAPE_STRING = "/";

    private static final Logger log = LoggerFactory.getLogger(ChatAction.class);
    private static final String DESC = "ChatAction";

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);
    private final AdminCommandAction adminCommandAction;

    public ChatAction(AdminCommandAction adminCommandAction) {
        this.adminCommandAction = adminCommandAction;
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
        if (!(message instanceof Chat_Request))
            throw new FatalActionException("Received incorrect instance of Chat: " + message);

        if (((Chat) message).getMessage().startsWith(ADMIN_COMMAND_ESCAPE_STRING)) {
            try {
                adminCommandAction.performAction(message, clientHandler);
                return;
            } catch (FatalActionException e) {
                log.warn("Admin command failed, processing as chat: " + e.getMessage());
            }
        }

        actionCount.incrementAndGet();

        try {
            clientHandler.getUser().chat(((Chat) message).getMessage());
        } catch (ActionException e) {
            log.info("Chat Denied: " + clientHandler.getUser() + ": "
                    + ((Chat) message).getMessage());

            try {
                clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                        "server", EmuLang.getString("ChatAction.ChatDenied", e.getMessage())));
            } catch (MessageFormatException e2) {
                log.error("Failed to contruct InformationMessage message: " + e.getMessage(), e);
            }
        }
    }

    public void handleEvent(ServerEvent event, V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        try {
            ChatEvent chatEvent = (ChatEvent) event;
            clientHandler.send(new Chat_Notification(clientHandler.getNextMessageNumber(),
                    chatEvent.getUser().getName(), chatEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct Chat_Notification message: " + e.getMessage(), e);
        }
    }
}
