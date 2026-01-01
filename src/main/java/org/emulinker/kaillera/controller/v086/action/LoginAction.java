package org.emulinker.kaillera.controller.v086.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage;
import org.emulinker.kaillera.controller.v086.protocol.ServerACK;
import org.emulinker.kaillera.controller.v086.protocol.UserInformation;
import org.emulinker.kaillera.controller.v086.protocol.UserJoined;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.ServerEvent;
import org.emulinker.kaillera.model.event.UserJoinedEvent;
import org.emulinker.kaillera.model.impl.KailleraUserImpl;

public final class LoginAction implements V086Action, V086ServerEventHandler {
    private static final Logger log = LoggerFactory.getLogger(LoginAction.class);
    private static final String DESC = "LoginAction";
    private static LoginAction singleton = new LoginAction();

    public static LoginAction getInstance() {
        return singleton;
    }

    private final AtomicInteger actionCount = new AtomicInteger(0);
    private final AtomicInteger handledCount = new AtomicInteger(0);

    private LoginAction() {

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

        UserInformation userInfo = (UserInformation) message;
        KailleraUser user = clientHandler.getUser();
        user.setName(userInfo.getUserName());
        user.setClientType(userInfo.getClientType());
        user.setSocketAddress(clientHandler.getRemoteSocketAddress());
        user.setConnectionType(userInfo.getConnectionType());
        clientHandler.startSpeedTest();

        try {
            clientHandler.send(new ServerACK(clientHandler.getNextMessageNumber()));
        } catch (MessageFormatException e) {
            log.error("Failed to contruct ServerACK message: " + e.getMessage(), e);
        }
    }

    public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler) {
        handledCount.incrementAndGet();

        UserJoinedEvent userJoinedEvent = (UserJoinedEvent) event;

        try {
            KailleraUserImpl user = (KailleraUserImpl) userJoinedEvent.getUser();
            clientHandler.send(new UserJoined(clientHandler.getNextMessageNumber(), user.getName(),
                    user.getID(), user.getPing(), (byte) user.getConnectionType()));

            KailleraUserImpl thisUser = (KailleraUserImpl) clientHandler.getUser();
            if (thisUser.isEmuLinkerClient()
                    && thisUser.getAccess() == AccessManager.ACCESS_ADMIN) {
                if (!user.equals(thisUser))
                    clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                            "server",
                            ":USERINFO=" + user.getID() + ","
                                    + user.getConnectSocketAddress().getAddress().getHostAddress()
                                    + "," + user.getAccessStr()));
            }
        } catch (MessageFormatException e) {
            log.error("Failed to contruct UserJoined_Notification message: " + e.getMessage(), e);
        }
    }
}
