package org.emulinker.kaillera.controller.v086.action;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage;
import org.emulinker.kaillera.controller.v086.protocol.JoinGame_Notification;
import org.emulinker.kaillera.controller.v086.protocol.JoinGame_Request;
import org.emulinker.kaillera.controller.v086.protocol.PlayerInformation;
import org.emulinker.kaillera.controller.v086.protocol.QuitGame_Notification;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.event.UserJoinedGameEvent;
import org.emulinker.kaillera.model.exception.JoinGameException;
import org.emulinker.util.EmuLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JoinGameAction implements V086Action, V086GameEventHandler {
    private static final Logger log = LoggerFactory.getLogger(JoinGameAction.class);
    private static final String DESC = "JoinGameAction";
    private static JoinGameAction singleton = new JoinGameAction();

    public static JoinGameAction getInstance() {
        return singleton;
    }

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);

    private JoinGameAction() {

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
        if (!(message instanceof JoinGame_Request))
            throw new FatalActionException("Received incorrect instance of JoinGame: " + message);

        actionCount.incrementAndGet();

        JoinGame_Request joinGameRequest = (JoinGame_Request) message;

        try {
            clientHandler.getUser().joinGame(joinGameRequest.getGameID());
        } catch (JoinGameException e) {
            try {
                clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                        "server",
                        EmuLang.getString("JoinGameAction.JoinGameDenied") + e.getMessage()));
                clientHandler.send(new QuitGame_Notification(clientHandler.getNextMessageNumber(),
                        clientHandler.getUser().getName(), clientHandler.getUser().getID()));
            } catch (MessageFormatException e2) {
                log.error("Failed to contruct new Message", e);
            }
        }
    }

    public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        UserJoinedGameEvent userJoinedEvent = (UserJoinedGameEvent) event;
        KailleraUser thisUser = clientHandler.getUser();

        try {
            KailleraGame game = userJoinedEvent.getGame();
            KailleraUser user = userJoinedEvent.getUser();

            if (user.equals(thisUser)) {
                List<PlayerInformation.Player> players = new ArrayList<PlayerInformation.Player>();

                for (KailleraUser player : game.getPlayers()) {
                    if (!player.equals(thisUser))
                        players.add(new PlayerInformation.Player(player.getName(), player.getPing(),
                                player.getID(), player.getConnectionType()));
                }

                clientHandler
                        .send(new PlayerInformation(clientHandler.getNextMessageNumber(), players));
            }

            clientHandler.send(new JoinGame_Notification(clientHandler.getNextMessageNumber(),
                    game.getID(), 0, user.getName(), user.getPing(), user.getID(),
                    user.getConnectionType()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct JoinGame_Notification message: " + e.getMessage(), e);
        }
    }
}
