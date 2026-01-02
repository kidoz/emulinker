package org.emulinker.kaillera.controller.v086.action;

import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.GameChat;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.exception.ActionException;
import org.emulinker.kaillera.model.impl.KailleraGameImpl;
import org.emulinker.kaillera.model.impl.KailleraUserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.emulinker.util.EmuLang;

public final class GameOwnerCommandAction implements V086Action {
    public static final String COMMAND_HELP = "/help"; //$NON-NLS-1$
    public static final String COMMAND_DETECTAUTOFIRE = "/detectautofire"; //$NON-NLS-1$

    private static final Logger log = LoggerFactory.getLogger(GameOwnerCommandAction.class);
    private static final String DESC = "GameOwnerCommandAction"; //$NON-NLS-1$

    private final AtomicInteger actionCount = new AtomicInteger(0);

    public GameOwnerCommandAction() {
    }

    public int getActionPerformedCount() {
        return actionCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
            throws FatalActionException {
        GameChat chatMessage = (GameChat) message;
        String chat = chatMessage.getMessage();

        KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
        KailleraGameImpl game = user.getGame();

        if (game == null) {
            throw new FatalActionException("GameOwner Command Failed: Not in a game: " + chat); //$NON-NLS-1$
        }

        if (!user.equals(game.getOwner())) {
            log.warn("GameOwner Command Denied: Not game owner: " + game + ": " + user + ": " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    + chat);
            return;
        }

        try {
            if (chat.startsWith(COMMAND_HELP)) {
                processHelp(chat, game, user, clientHandler);
            } else if (chat.startsWith(COMMAND_DETECTAUTOFIRE)) {
                processDetectAutoFire(chat, game, user, clientHandler);
            } else {
                log.info("Unknown GameOwner Command: " + game + ": " + user + ": " + chat); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        } catch (ActionException e) {
            log.info("GameOwner Command Failed: " + game + ": " + user + ": " + chat); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            game.announce(
                    EmuLang.getString("GameOwnerCommandAction.CommandFailed", e.getMessage())); //$NON-NLS-1$
        } catch (MessageFormatException e) {
            log.error("Failed to contruct message: " + e.getMessage(), e); //$NON-NLS-1$
        }
    }

    private void processHelp(String message, KailleraGameImpl game, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        game.announce(EmuLang.getString("GameOwnerCommandAction.AvailableCommands")); //$NON-NLS-1$
        game.announce(EmuLang.getString("GameOwnerCommandAction.SetAutofireDetection")); //$NON-NLS-1$
    }

    private void autoFireHelp(KailleraGameImpl game) {
        int cur = game.getAutoFireDetector().getSensitivity();
        game.announce(EmuLang.getString("GameOwnerCommandAction.HelpSensitivity")); //$NON-NLS-1$
        game.announce(EmuLang.getString("GameOwnerCommandAction.HelpDisable")); //$NON-NLS-1$
        game.announce(EmuLang.getString("GameOwnerCommandAction.HelpCurrentSensitivity", cur) //$NON-NLS-1$
                + (cur == 0 ? (EmuLang.getString("GameOwnerCommandAction.HelpDisabled")) : "")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void processDetectAutoFire(String message, KailleraGameImpl game,
            KailleraUserImpl admin, V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        if (game.getStatus() != KailleraGame.STATUS_WAITING) {
            game.announce(EmuLang.getString("GameOwnerCommandAction.AutoFireChangeDeniedInGame")); //$NON-NLS-1$
            return;
        }

        StringTokenizer st = new StringTokenizer(message, " "); //$NON-NLS-1$
        if (st.countTokens() != 2) {
            autoFireHelp(game);
            return;
        }

        String command = st.nextToken();
        String sensitivityStr = st.nextToken();
        int sensitivity = -1;

        try {
            sensitivity = Integer.parseInt(sensitivityStr);
        } catch (NumberFormatException e) {
        }

        if (sensitivity > 5 || sensitivity < 0) {
            autoFireHelp(game);
            return;
        }

        game.getAutoFireDetector().setSensitivity(sensitivity);
        game.announce(
                EmuLang.getString("GameOwnerCommandAction.HelpCurrentSensitivity", sensitivity) //$NON-NLS-1$
                        + (sensitivity == 0
                                ? (EmuLang.getString("GameOwnerCommandAction.HelpDisabled")) //$NON-NLS-1$
                                : "")); //$NON-NLS-1$
    }
}
