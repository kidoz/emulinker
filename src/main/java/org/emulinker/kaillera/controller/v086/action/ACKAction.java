package org.emulinker.kaillera.controller.v086.action;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.ConnectionRejected;
import org.emulinker.kaillera.controller.v086.protocol.ServerACK;
import org.emulinker.kaillera.controller.v086.protocol.ServerStatus;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.ConnectedEvent;
import org.emulinker.kaillera.model.event.UserEvent;
import org.emulinker.kaillera.model.exception.LoginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ACKAction implements V086Action, V086UserEventHandler {
    private static final Logger log = LoggerFactory.getLogger(ACKAction.class);
    private static final String DESC = "ACKAction";
    private static int numAcksForSpeedTest = 3;

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);

    public ACKAction() {
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
        actionCount.incrementAndGet();

        KailleraUser user = clientHandler.getUser();
        if (user.isLoggedIn())
            return;

        clientHandler.addSpeedMeasurement();

        if (clientHandler.getSpeedMeasurementCount() > numAcksForSpeedTest) {
            user.setPing(clientHandler.getAverageNetworkSpeed());
            user.setPing(clientHandler.getBestNetworkSpeed());

            log.debug("Calculated " + user + " ping time: average="
                    + clientHandler.getAverageNetworkSpeed() + ", best="
                    + clientHandler.getBestNetworkSpeed());

            try {
                user.login();
            } catch (LoginException e) {
                try {
                    clientHandler.send(new ConnectionRejected(clientHandler.getNextMessageNumber(),
                            user.getName(), user.getID(), e.getMessage()));
                } catch (MessageFormatException e2) {
                    log.error("Failed to contruct new ConnectionRejected", e);
                }

                throw new FatalActionException("Login failed: " + e.getMessage());
            }
        } else {
            try {
                clientHandler.send(new ServerACK(clientHandler.getNextMessageNumber()));
            } catch (MessageFormatException e) {
                log.error("Failed to contruct new ServerACK", e);
                return;
            }
        }
    }

    public void handleEvent(UserEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        ConnectedEvent connectedEvent = (ConnectedEvent) event;

        KailleraServer server = connectedEvent.getServer();
        KailleraUser thisUser = connectedEvent.getUser();

        List<ServerStatus.User> users = new ArrayList<ServerStatus.User>();
        List<ServerStatus.Game> games = new ArrayList<ServerStatus.Game>();

        try {
            for (KailleraUser user : server.getUsers()) {
                if (user.getStatus() != KailleraUser.STATUS_CONNECTING && !user.equals(thisUser))
                    users.add(new ServerStatus.User(user.getName(), user.getPing(),
                            (byte) user.getStatus(), user.getID(), user.getConnectionType()));
            }
        } catch (MessageFormatException e) {
            log.error("Failed to contruct new ServerStatus.User", e);
            return;
        }

        try {
            for (KailleraGame game : server.getGames())
                games.add(new ServerStatus.Game(game.getRomName(), (short) game.getID(),
                        game.getClientType(), game.getOwner().getName(),
                        (game.getNumPlayers() + "/" + 2), (byte) game.getStatus()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct new ServerStatus.User", e);
            return;
        }

        // Here I am attempting to fix the inherent Kaillera protocol bug that occurs
        // when there are a large number of users
        // and/or games on the server. The size of the ServerStatus packet can be very
        // large, and depending on the router
        // settings or os config, the packet size exceeds a UDP/IP limit and gets
        // dropped. This results in the user getting
        // half logged-in, in a weird state.

        // I am attempting to fix this by breaking the ServerStatus message up into
        // multiple packets. I'm shooting for a max
        // packet size of 1500 bytes, but since kaillera sends 3 messages per packet,
        // the max size for a single message should be 500

        int counter = 0;
        boolean sent = false;

        List<ServerStatus.User> usersSubList = new ArrayList<ServerStatus.User>();
        List<ServerStatus.Game> gamesSubList = new ArrayList<ServerStatus.Game>();

        while (!users.isEmpty()) {
            ServerStatus.User user = users.get(0);
            users.remove(0);

            if ((counter + user.getLength()) >= 400) {
                sendServerStatus(clientHandler, usersSubList, gamesSubList, counter);
                usersSubList = new ArrayList<ServerStatus.User>();
                gamesSubList = new ArrayList<ServerStatus.Game>();
                counter = 0;
                sent = true;
            }

            counter += user.getLength();
            usersSubList.add(user);
        }

        while (!games.isEmpty()) {
            ServerStatus.Game game = games.get(0);
            games.remove(0);

            if ((counter + game.getLength()) >= 400) {
                sendServerStatus(clientHandler, usersSubList, gamesSubList, counter);
                usersSubList = new ArrayList<ServerStatus.User>();
                gamesSubList = new ArrayList<ServerStatus.Game>();
                counter = 0;
                sent = true;
            }

            counter += game.getLength();
            gamesSubList.add(game);
        }

        if ((usersSubList.size() > 0 || gamesSubList.size() > 0) || !sent)
            sendServerStatus(clientHandler, usersSubList, gamesSubList, counter);
    }

    private void sendServerStatus(V086Controller.V086ClientHandler clientHandler,
            List<ServerStatus.User> users, List<ServerStatus.Game> games, int counter) {
        StringBuilder sb = new StringBuilder();
        for (ServerStatus.Game game : games) {
            sb.append(game.getGameID());
            sb.append(",");
        }
        log.debug("Sending ServerStatus to " + clientHandler.getUser() + ": " + users.size()
                + " users, " + games.size() + " games in " + counter + " bytes, games: "
                + sb.toString());
        try {
            clientHandler
                    .send(new ServerStatus(clientHandler.getNextMessageNumber(), users, games));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct new ServerStatus for users", e);
        }
    }
}
