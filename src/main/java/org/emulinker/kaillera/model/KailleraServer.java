package org.emulinker.kaillera.model;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.exception.ChatException;
import org.emulinker.kaillera.model.exception.ClientAddressException;
import org.emulinker.kaillera.model.exception.CloseGameException;
import org.emulinker.kaillera.model.exception.ConnectionTypeException;
import org.emulinker.kaillera.model.exception.CreateGameException;
import org.emulinker.kaillera.model.exception.DropGameException;
import org.emulinker.kaillera.model.exception.FloodException;
import org.emulinker.kaillera.model.exception.LoginException;
import org.emulinker.kaillera.model.exception.NewConnectionException;
import org.emulinker.kaillera.model.exception.PingTimeException;
import org.emulinker.kaillera.model.exception.QuitException;
import org.emulinker.kaillera.model.exception.QuitGameException;
import org.emulinker.kaillera.model.exception.ServerFullException;
import org.emulinker.kaillera.model.exception.UserNameException;
import org.emulinker.release.ReleaseInfo;

public interface KailleraServer {

    void start();

    void stop();
    ReleaseInfo getReleaseInfo();

    int getNumUsers();

    int getNumGames();

    int getMaxUsers();

    int getMaxGames();

    int getMaxPing();

    Collection<? extends KailleraUser> getUsers();

    Collection<? extends KailleraGame> getGames();

    KailleraUser getUser(int userID);

    KailleraGame getGame(int gameID);

    KailleraUser newConnection(InetSocketAddress clientSocketAddress, String protocol,
            KailleraEventListener listener) throws ServerFullException, NewConnectionException;

    void login(KailleraUser user) throws PingTimeException, ClientAddressException,
            ConnectionTypeException, UserNameException, LoginException;

    void chat(KailleraUser user, String message) throws ChatException, FloodException;

    KailleraGame createGame(KailleraUser user, String romName)
            throws CreateGameException, FloodException;

    void quit(KailleraUser user, String message)
            throws QuitException, DropGameException, QuitGameException, CloseGameException;
}
