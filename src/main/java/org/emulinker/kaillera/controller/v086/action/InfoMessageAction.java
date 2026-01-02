package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage;
import org.emulinker.kaillera.model.event.InfoMessageEvent;
import org.emulinker.kaillera.model.event.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InfoMessageAction implements V086UserEventHandler {
    private static final Logger log = LoggerFactory.getLogger(InfoMessageAction.class);
    private static final String DESC = "InfoMessageAction";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public InfoMessageAction() {
    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void handleEvent(UserEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        InfoMessageEvent infoEvent = (InfoMessageEvent) event;

        try {
            clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                    "server", infoEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct InformationMessage message: " + e.getMessage(), e);
        }
    }
}
