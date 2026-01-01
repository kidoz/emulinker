package org.emulinker.kaillera.controller.v086.action;

import java.net.InetAddress;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.Chat;
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.exception.ActionException;
import org.emulinker.kaillera.model.impl.KailleraGameImpl;
import org.emulinker.kaillera.model.impl.KailleraServerImpl;
import org.emulinker.kaillera.model.impl.KailleraUserImpl;
import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.EmuLang;
import org.emulinker.util.EmuUtil;
import org.emulinker.util.WildcardStringPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminCommandAction implements V086Action {
    public static final String COMMAND_ANNOUNCE = "/announce"; //$NON-NLS-1$
    public static final String COMMAND_ANNOUNCEALL = "/announceall"; //$NON-NLS-1$
    public static final String COMMAND_ANNOUNCEGAME = "/announcegame"; //$NON-NLS-1$
    public static final String COMMAND_BAN = "/ban"; //$NON-NLS-1$
    public static final String COMMAND_CLEAR = "/clear"; //$NON-NLS-1$
    public static final String COMMAND_CLOSEGAME = "/closegame"; //$NON-NLS-1$
    public static final String COMMAND_FINDGAME = "/findgame"; //$NON-NLS-1$
    public static final String COMMAND_FINDUSER = "/finduser"; //$NON-NLS-1$
    public static final String COMMAND_HELP = "/help"; //$NON-NLS-1$
    public static final String COMMAND_KICK = "/kick"; //$NON-NLS-1$
    public static final String COMMAND_SILENCE = "/silence"; //$NON-NLS-1$
    public static final String COMMAND_TEMPADMIN = "/tempadmin"; //$NON-NLS-1$
    public static final String COMMAND_VERSION = "/version"; //$NON-NLS-1$

    private static final Logger log = LoggerFactory.getLogger(AdminCommandAction.class);
    private static final String DESC = "AdminCommandAction"; //$NON-NLS-1$
    private static AdminCommandAction singleton = new AdminCommandAction();

    public static AdminCommandAction getInstance() {
        return singleton;
    }

    private final AtomicInteger actionCount = new AtomicInteger(0);

    private AdminCommandAction() {

    }

    public int getActionPerformedCount() {
        return actionCount.get();
    }

    public String toString() {
        return DESC;
    }

    public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
            throws FatalActionException {
        Chat chatMessage = (Chat) message;
        String chat = chatMessage.getMessage();
        KailleraServerImpl server = (KailleraServerImpl) clientHandler.getController().getServer();
        AccessManager accessManager = server.getAccessManager();
        KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
        if (accessManager
                .getAccess(clientHandler.getRemoteInetAddress()) != AccessManager.ACCESS_ADMIN)
            throw new FatalActionException(
                    "Admin Command Denied: " + user + " does not have Admin access: " + chat); //$NON-NLS-1$ //$NON-NLS-2$

        log.info(user + ": Admin Command: " + chat); //$NON-NLS-1$

        try {
            if (chat.startsWith(COMMAND_HELP)) {
                processHelp(chat, server, user, clientHandler);
            } else if (chat.startsWith(COMMAND_FINDUSER)) {
                processFindUser(chat, server, user, clientHandler);
            } else if (chat.startsWith(COMMAND_FINDGAME)) {
                processFindGame(chat, server, user, clientHandler);
            } else if (chat.startsWith(COMMAND_CLOSEGAME)) {
                processCloseGame(chat, server, user, clientHandler);
            } else if (chat.startsWith(COMMAND_KICK)) {
                processKick(chat, server, user, clientHandler);
            } else if (chat.startsWith(COMMAND_BAN)) {
                processBan(chat, server, user, clientHandler);
            } else if (chat.startsWith(COMMAND_ANNOUNCEGAME)) {
                processGameAnnounce(chat, server, user, clientHandler);
            } else if (chat.startsWith(COMMAND_ANNOUNCE)) {
                processAnnounce(chat, server, user, clientHandler);
            } else if (chat.startsWith(COMMAND_SILENCE)) {
                processSilence(chat, server, user, clientHandler);
            } else if (chat.startsWith(COMMAND_TEMPADMIN)) {
                processTempAdmin(chat, server, user, clientHandler);
            } else if (chat.startsWith(COMMAND_VERSION)) {
                processVersion(chat, server, user, clientHandler);
            } else if (chat.startsWith(COMMAND_CLEAR)) {
                processClear(chat, server, user, clientHandler);
            } else
                throw new ActionException("Invalid Command: " + chat); //$NON-NLS-1$
        } catch (ActionException e) {
            log.info("Admin Command Failed: " + user + ": " + chat); //$NON-NLS-1$ //$NON-NLS-2$

            try {
                clientHandler
                        .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                                EmuLang.getString("AdminCommandAction.Failed", e.getMessage()))); //$NON-NLS-1$
            } catch (MessageFormatException e2) {
                log.error("Failed to contruct InformationMessage message: " + e.getMessage(), e); //$NON-NLS-1$
            }
        } catch (MessageFormatException e) {
            log.error("Failed to contruct message: " + e.getMessage(), e); //$NON-NLS-1$
        }
    }

    private void processHelp(String message, KailleraServerImpl server, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.AdminCommands"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpVersion"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpTempAdmin"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpKick"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpBan"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpSilence"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpClear"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpCloseGame"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpAnnounce"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpAnnounceAll"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpAnnounceGame"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpFindUser"))); //$NON-NLS-1$
        clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                EmuLang.getString("AdminCommandAction.HelpFindGame"))); //$NON-NLS-1$
    }

    private void processFindUser(String message, KailleraServerImpl server, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        int space = message.indexOf(' ');
        if (space < 0)
            throw new ActionException(EmuLang.getString("AdminCommandAction.FindUserError")); //$NON-NLS-1$

        int foundCount = 0;
        WildcardStringPattern pattern = new WildcardStringPattern(message.substring(space + 1));
        for (KailleraUserImpl user : server.getUsers()) {
            if (!user.isLoggedIn())
                continue;

            if (pattern.match(user.getName())) {
                StringBuilder sb = new StringBuilder();
                sb.append(user.getID());
                sb.append(": "); //$NON-NLS-1$
                sb.append(user.getPing());
                sb.append("ms "); //$NON-NLS-1$
                sb.append(user.getConnectSocketAddress().getAddress().getHostAddress());
                sb.append(" "); //$NON-NLS-1$
                sb.append(user.getName());
                clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                        "server", sb.toString())); //$NON-NLS-1$
                foundCount++;
            }
        }

        if (foundCount == 0)
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            EmuLang.getString("AdminCommandAction.NoUsersFound"))); //$NON-NLS-1$
    }

    private void processFindGame(String message, KailleraServerImpl server, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        int space = message.indexOf(' ');
        if (space < 0)
            throw new ActionException(EmuLang.getString("AdminCommandAction.FindGameError")); //$NON-NLS-1$

        int foundCount = 0;
        WildcardStringPattern pattern = new WildcardStringPattern(message.substring(space + 1));
        for (KailleraGameImpl game : server.getGames()) {
            if (pattern.match(game.getRomName())) {
                StringBuilder sb = new StringBuilder();
                sb.append(game.getID());
                sb.append(": "); //$NON-NLS-1$
                sb.append(game.getOwner().getName());
                sb.append(" "); //$NON-NLS-1$
                sb.append(game.getRomName());
                clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                        "server", sb.toString())); //$NON-NLS-1$
                foundCount++;
            }
        }

        if (foundCount == 0)
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            EmuLang.getString("AdminCommandAction.NoGamesFound"))); //$NON-NLS-1$
    }

    private void processSilence(String message, KailleraServerImpl server, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        Scanner scanner = new Scanner(message).useDelimiter(" "); //$NON-NLS-1$

        try {
            scanner.next();
            int userID = scanner.nextInt();
            int minutes = scanner.nextInt();

            KailleraUserImpl user = (KailleraUserImpl) server.getUser(userID);
            if (user == null)
                throw new ActionException(
                        EmuLang.getString("AdminCommandAction.UserNotFound") + userID); //$NON-NLS-1$

            if (user.getID() == admin.getID())
                throw new ActionException(
                        EmuLang.getString("AdminCommandAction.CanNotSilenceSelf")); //$NON-NLS-1$

            int access = server.getAccessManager()
                    .getAccess(user.getConnectSocketAddress().getAddress());
            if (access == AccessManager.ACCESS_ADMIN)
                throw new ActionException(
                        EmuLang.getString("AdminCommandAction.CanNotSilenceAdmin")); //$NON-NLS-1$

            server.getAccessManager().addSilenced(
                    user.getConnectSocketAddress().getAddress().getHostAddress(), minutes);
            server.announce(
                    EmuLang.getString("AdminCommandAction.Silenced", minutes, user.getName()), //$NON-NLS-1$
                    false); // $NON-NLS-2$
        } catch (NoSuchElementException e) {
            throw new ActionException(EmuLang.getString("AdminCommandAction.SilenceError")); //$NON-NLS-1$
        }
    }

    private void processKick(String message, KailleraServerImpl server, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        Scanner scanner = new Scanner(message).useDelimiter(" "); //$NON-NLS-1$

        try {
            scanner.next();
            int userID = scanner.nextInt();

            KailleraUserImpl user = (KailleraUserImpl) server.getUser(userID);
            if (user == null)
                throw new ActionException(
                        EmuLang.getString("AdminCommandAction.UserNotFound", userID)); //$NON-NLS-1$

            if (user.getID() == admin.getID())
                throw new ActionException(EmuLang.getString("AdminCommandAction.CanNotKickSelf")); //$NON-NLS-1$

            int access = server.getAccessManager()
                    .getAccess(user.getConnectSocketAddress().getAddress());
            if (access == AccessManager.ACCESS_ADMIN)
                throw new ActionException(EmuLang.getString("AdminCommandAction.CanNotKickAdmin")); //$NON-NLS-1$

            user.quit(EmuLang.getString("AdminCommandAction.QuitKicked")); //$NON-NLS-1$
        } catch (NoSuchElementException e) {
            throw new ActionException(EmuLang.getString("AdminCommandAction.KickError")); //$NON-NLS-1$
        }
    }

    private void processCloseGame(String message, KailleraServerImpl server, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        Scanner scanner = new Scanner(message).useDelimiter(" "); //$NON-NLS-1$

        try {
            scanner.next();
            int gameID = scanner.nextInt();

            KailleraGameImpl game = (KailleraGameImpl) server.getGame(gameID);
            if (game == null)
                throw new ActionException(
                        EmuLang.getString("AdminCommandAction.GameNotFound", gameID)); //$NON-NLS-1$

            KailleraUserImpl owner = (KailleraUserImpl) game.getOwner();
            int access = server.getAccessManager()
                    .getAccess(owner.getConnectSocketAddress().getAddress());

            if (access == AccessManager.ACCESS_ADMIN && owner.isLoggedIn())
                throw new ActionException(
                        EmuLang.getString("AdminCommandAction.CanNotCloseAdminGame")); //$NON-NLS-1$

            owner.quitGame();
        } catch (NoSuchElementException e) {
            throw new ActionException(EmuLang.getString("AdminCommandAction.CloseGameError")); //$NON-NLS-1$
        }
    }

    private void processBan(String message, KailleraServerImpl server, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        Scanner scanner = new Scanner(message).useDelimiter(" "); //$NON-NLS-1$

        try {
            scanner.next();
            int userID = scanner.nextInt();
            int minutes = scanner.nextInt();

            KailleraUserImpl user = (KailleraUserImpl) server.getUser(userID);
            if (user == null)
                throw new ActionException(
                        EmuLang.getString("AdminCommandAction.UserNotFound", userID)); //$NON-NLS-1$

            if (user.getID() == admin.getID())
                throw new ActionException(EmuLang.getString("AdminCommandAction.CanNotBanSelf")); //$NON-NLS-1$

            int access = server.getAccessManager()
                    .getAccess(user.getConnectSocketAddress().getAddress());
            if (access == AccessManager.ACCESS_ADMIN)
                throw new ActionException(EmuLang.getString("AdminCommandAction.CanNotBanAdmin")); //$NON-NLS-1$

            user.quit(EmuLang.getString("AdminCommandAction.QuitBanned")); //$NON-NLS-1$

            server.getAccessManager().addTempBan(
                    user.getConnectSocketAddress().getAddress().getHostAddress(), minutes);
            server.announce(EmuLang.getString("AdminCommandAction.Banned", minutes, user.getName()), //$NON-NLS-1$
                    false); // $NON-NLS-2$
        } catch (NoSuchElementException e) {
            throw new ActionException(EmuLang.getString("AdminCommandAction.BanError")); //$NON-NLS-1$
        }
    }

    private void processTempAdmin(String message, KailleraServerImpl server, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        Scanner scanner = new Scanner(message).useDelimiter(" "); //$NON-NLS-1$

        try {
            scanner.next();
            int userID = scanner.nextInt();
            int minutes = scanner.nextInt();

            KailleraUserImpl user = (KailleraUserImpl) server.getUser(userID);
            if (user == null)
                throw new ActionException(
                        EmuLang.getString("AdminCommandAction.UserNotFound", userID)); //$NON-NLS-1$

            if (user.getID() == admin.getID())
                throw new ActionException(EmuLang.getString("AdminCommandAction.AlreadyAdmin")); //$NON-NLS-1$

            int access = server.getAccessManager()
                    .getAccess(user.getConnectSocketAddress().getAddress());
            if (access == AccessManager.ACCESS_ADMIN)
                throw new ActionException(EmuLang.getString("AdminCommandAction.UserAlreadyAdmin")); //$NON-NLS-1$

            server.getAccessManager().addTempAdmin(
                    user.getConnectSocketAddress().getAddress().getHostAddress(), minutes);
            server.announce(EmuLang.getString("AdminCommandAction.TempAdminGranted", minutes, //$NON-NLS-1$
                    user.getName()), false); // $NON-NLS-2$
        } catch (NoSuchElementException e) {
            throw new ActionException(EmuLang.getString("AdminCommandAction.TempAdminError")); //$NON-NLS-1$
        }
    }

    private void processAnnounce(String message, KailleraServerImpl server, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        int space = message.indexOf(' ');
        if (space < 0)
            throw new ActionException(EmuLang.getString("AdminCommandAction.AnnounceError")); //$NON-NLS-1$

        boolean all = false;
        if (message.startsWith(COMMAND_ANNOUNCEALL))
            all = true;

        String announcement = message.substring(space + 1);
        if (announcement.startsWith(":"))
            announcement = announcement.substring(1); // this protects against people screwing up
                                                      // the emulinker
                                                      // supraclient

        server.announce(announcement, all);
    }

    private void processGameAnnounce(String message, KailleraServerImpl server,
            KailleraUserImpl admin, V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        Scanner scanner = new Scanner(message).useDelimiter(" "); //$NON-NLS-1$

        try {
            scanner.next();
            int gameID = scanner.nextInt();

            StringBuilder sb = new StringBuilder();
            while (scanner.hasNext()) {
                sb.append(scanner.next());
                sb.append(" "); //$NON-NLS-1$
            }

            KailleraGameImpl game = (KailleraGameImpl) server.getGame(gameID);
            if (game == null)
                throw new ActionException(
                        EmuLang.getString("AdminCommandAction.GameNoutFound") + gameID); //$NON-NLS-1$

            game.announce(sb.toString());
        } catch (NoSuchElementException e) {
            throw new ActionException(EmuLang.getString("AdminCommandAction.AnnounceGameError")); //$NON-NLS-1$
        }
    }

    private void processClear(String message, KailleraServerImpl server, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        int space = message.indexOf(' ');
        if (space < 0)
            throw new ActionException(EmuLang.getString("AdminCommandAction.ClearError"));

        String addressStr = message.substring(space + 1);
        InetAddress inetAddr = null;
        try {
            inetAddr = InetAddress.getByName(addressStr);
        } catch (Exception e) {
            throw new ActionException(
                    EmuLang.getString("AdminCommandAction.ClearAddressFormatError"));
        }

        if (server.getAccessManager().clearTemp(inetAddr))
            clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                    "server", EmuLang.getString("AdminCommandAction.ClearSuccess")));
        else
            clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(),
                    "server", EmuLang.getString("AdminCommandAction.ClearNotFound")));
    }

    private void processVersion(String message, KailleraServerImpl server, KailleraUserImpl admin,
            V086Controller.V086ClientHandler clientHandler)
            throws ActionException, MessageFormatException {
        try {
            ReleaseInfo releaseInfo = server.getReleaseInfo();
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            "VERSION: " + releaseInfo.getVersionString())); //$NON-NLS-1$
            sleep(20);

            Properties props = System.getProperties();
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            "JAVAVER: " + props.getProperty("java.version"))); //$NON-NLS-1$ //$NON-NLS-2$
            sleep(20);
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            "JAVAVEND: " + props.getProperty("java.vendor"))); //$NON-NLS-1$ //$NON-NLS-2$
            sleep(20);
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            "OSNAME: " + props.getProperty("os.name"))); //$NON-NLS-1$ //$NON-NLS-2$
            sleep(20);
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            "OSARCH: " + props.getProperty("os.arch"))); //$NON-NLS-1$ //$NON-NLS-2$
            sleep(20);
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            "OSVER: " + props.getProperty("os.version"))); //$NON-NLS-1$ //$NON-NLS-2$
            sleep(20);

            Runtime runtime = Runtime.getRuntime();
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            "NUMPROCS: " + runtime.availableProcessors())); //$NON-NLS-1$
            sleep(20);
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            "FREEMEM: " + runtime.freeMemory())); //$NON-NLS-1$
            sleep(20);
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            "MAXMEM: " + runtime.maxMemory())); //$NON-NLS-1$
            sleep(20);
            clientHandler
                    .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                            "TOTMEM: " + runtime.totalMemory())); //$NON-NLS-1$
            sleep(20);

            Map<String, String> env = System.getenv();

            if (EmuUtil.systemIsWindows()) {
                clientHandler
                        .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                                "COMPNAME: " + env.get("COMPUTERNAME"))); //$NON-NLS-1$ //$NON-NLS-2$
                sleep(20);
                clientHandler
                        .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                                "USER: " + env.get("USERNAME"))); //$NON-NLS-1$ //$NON-NLS-2$
                sleep(20);
            } else {
                clientHandler
                        .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                                "COMPNAME: " + env.get("HOSTNAME"))); //$NON-NLS-1$ //$NON-NLS-2$
                sleep(20);
                clientHandler
                        .send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", //$NON-NLS-1$
                                "USER: " + env.get("USERNAME"))); //$NON-NLS-1$ //$NON-NLS-2$
                sleep(20);
            }
        } catch (NoSuchElementException e) {
            throw new ActionException(EmuLang.getString("AdminCommandAction.VersionError")); //$NON-NLS-1$
        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
        }
    }
}
