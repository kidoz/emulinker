package org.emulinker.kaillera.model;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.emulinker.release.*;
import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.exception.*;
import org.picocontainer.Startable;

public interface KailleraServer extends Startable {
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
