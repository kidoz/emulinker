package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086GameEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086GameEvent;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.GameEvent;
import su.kidoz.kaillera.model.event.GameTimeoutEvent;

@Component
@V086GameEvent(eventType = GameTimeoutEvent.class)
public final class GameTimeoutAction implements V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(GameTimeoutAction.class);
    private static final String DESC = "GameTimeoutAction";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public GameTimeoutAction() {
    }

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void handleEvent(GameEvent event, V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        GameTimeoutEvent timeoutEvent = (GameTimeoutEvent) event;
        KailleraUser player = timeoutEvent.getUser();
        KailleraUser user = clientHandler.getUser();

        if (player.equals(user)) {
            log.debug(user + " received timeout event " + timeoutEvent.getTimeoutNumber() + " for "
                    + timeoutEvent.getGame() + ": resending messages...");
            clientHandler.resend(timeoutEvent.getTimeoutNumber());
        } else {
            log.debug(user + " received timeout event " + timeoutEvent.getTimeoutNumber() + " from "
                    + player + " for " + timeoutEvent.getGame());
        }
    }
}
