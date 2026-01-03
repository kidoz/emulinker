package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086UserEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086UserEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.InformationMessage;
import su.kidoz.kaillera.model.event.InfoMessageEvent;
import su.kidoz.kaillera.model.event.UserEvent;

@Component
@V086UserEvent(eventType = InfoMessageEvent.class)
public final class InfoMessageEventRenderer implements V086UserEventHandler {
    private static final Logger log = LoggerFactory.getLogger(InfoMessageEventRenderer.class);
    private static final String DESC = "InfoMessageEventRenderer";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public InfoMessageEventRenderer() {
    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    @Override
    public void handleEvent(UserEvent event, V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        InfoMessageEvent infoEvent = (InfoMessageEvent) event;

        try {
            clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                    "server", infoEvent.getMessage()));
        } catch (MessageFormatException e) {
            log.error("Failed to construct InformationMessage message: " + e.getMessage(), e);
        }
    }
}
