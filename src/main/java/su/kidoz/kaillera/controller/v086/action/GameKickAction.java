package su.kidoz.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.GameKick;
import su.kidoz.kaillera.controller.v086.protocol.V086Message;
import su.kidoz.kaillera.model.exception.GameKickException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameKickAction implements V086Action {
    private static final Logger log = LoggerFactory.getLogger(GameKickAction.class);
    private static final String DESC = "GameKickAction";

    private final AtomicInteger actionCount = new AtomicInteger(0);

    public GameKickAction() {
    }

    public int getActionPerformedCount() {
        return actionCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void performAction(V086Message message, V086ClientHandler clientHandler)
            throws FatalActionException {
        actionCount.incrementAndGet();

        GameKick kickRequest = (GameKick) message;

        try {
            clientHandler.getUser().gameKick(kickRequest.getUserID());
        } catch (GameKickException e) {
            log.debug("Failed to kick: " + e.getMessage());
        }
    }
}
