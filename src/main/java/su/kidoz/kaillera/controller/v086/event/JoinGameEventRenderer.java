package su.kidoz.kaillera.controller.v086.event;

import su.kidoz.kaillera.controller.v086.action.V086GameEventHandler;
import su.kidoz.kaillera.controller.v086.annotation.V086GameEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.kidoz.kaillera.controller.messaging.MessageFormatException;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.protocol.JoinGame_Notification;
import su.kidoz.kaillera.controller.v086.protocol.PlayerInformation;
import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.GameEvent;
import su.kidoz.kaillera.model.event.UserJoinedGameEvent;

/**
 * Event renderer for outbound user joined game notifications.
 *
 * <p>
 * Renders UserJoinedGameEvent domain events into JoinGame_Notification protocol
 * messages. Inbound command handling is done by {@link JoinGameCommandAction}.
 */
@Component
@V086GameEvent(eventType = UserJoinedGameEvent.class)
public final class JoinGameEventRenderer implements V086GameEventHandler {

    private static final Logger log = LoggerFactory.getLogger(JoinGameEventRenderer.class);
    private static final String DESC = "JoinGameEventRenderer";

    private final AtomicInteger handledCount = new AtomicInteger(0);

    public int getHandledEventCount() {
        return handledCount.get();
    }

    public String toString() {
        return DESC;
    }

    @Override
    public void handleEvent(final GameEvent event, final V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        UserJoinedGameEvent userJoinedEvent = (UserJoinedGameEvent) event;
        KailleraUser thisUser = clientHandler.getUser();

        try {
            KailleraGame game = userJoinedEvent.getGame();
            KailleraUser user = userJoinedEvent.getUser();

            if (user.equals(thisUser)) {
                List<PlayerInformation.Player> players = new ArrayList<>();

                for (KailleraUser player : game.getPlayers()) {
                    if (!player.equals(thisUser)) {
                        players.add(new PlayerInformation.Player(player.getName(), player.getPing(),
                                player.getID(), player.getConnectionType()));
                    }
                }

                clientHandler
                        .send(new PlayerInformation(clientHandler.getNextMessageNumber(), players));
            }

            clientHandler.send(new JoinGame_Notification(clientHandler.getNextMessageNumber(),
                    game.getID(), 0, user.getName(), user.getPing(), user.getID(),
                    user.getConnectionType()));
        } catch (MessageFormatException e) {
            log.error("Failed to construct JoinGame_Notification message: " + e.getMessage(), e);
        }
    }
}
