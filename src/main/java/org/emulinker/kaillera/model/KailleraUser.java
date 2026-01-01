package org.emulinker.kaillera.model;

import java.net.InetSocketAddress;

import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.exception.ChatException;
import org.emulinker.kaillera.model.exception.ClientAddressException;
import org.emulinker.kaillera.model.exception.CloseGameException;
import org.emulinker.kaillera.model.exception.ConnectionTypeException;
import org.emulinker.kaillera.model.exception.CreateGameException;
import org.emulinker.kaillera.model.exception.DropGameException;
import org.emulinker.kaillera.model.exception.FloodException;
import org.emulinker.kaillera.model.exception.GameChatException;
import org.emulinker.kaillera.model.exception.GameDataException;
import org.emulinker.kaillera.model.exception.GameKickException;
import org.emulinker.kaillera.model.exception.JoinGameException;
import org.emulinker.kaillera.model.exception.LoginException;
import org.emulinker.kaillera.model.exception.PingTimeException;
import org.emulinker.kaillera.model.exception.QuitException;
import org.emulinker.kaillera.model.exception.QuitGameException;
import org.emulinker.kaillera.model.exception.StartGameException;
import org.emulinker.kaillera.model.exception.UserNameException;
import org.emulinker.kaillera.model.exception.UserReadyException;

public interface KailleraUser {
    byte CONNECTION_TYPE_LAN = 1;
    byte CONNECTION_TYPE_EXCELLENT = 2;
    byte CONNECTION_TYPE_GOOD = 3;
    byte CONNECTION_TYPE_AVERAGE = 4;
    byte CONNECTION_TYPE_LOW = 5;
    byte CONNECTION_TYPE_BAD = 6;

    String[] CONNECTION_TYPE_NAMES = {"DISABLED", "Lan", "Excellent", "Good", "Average", "Low",
            "Bad"};

    byte STATUS_PLAYING = 0;
    byte STATUS_IDLE = 1;
    byte STATUS_CONNECTING = 2;
    String[] STATUS_NAMES = {"Playing", "Idle", "Connecting"};

    int getID();

    InetSocketAddress getConnectSocketAddress();

    String getProtocol();

    long getConnectTime();

    int getStatus();

    String getName();

    void setName(String name);

    String getClientType();

    boolean isEmuLinkerClient();

    void setClientType(String clientType);

    byte getConnectionType();

    void setConnectionType(byte connectionType);

    InetSocketAddress getSocketAddress();

    void setSocketAddress(InetSocketAddress clientSocketAddress);

    int getPing();

    void setPing(int ping);

    void login() throws PingTimeException, ClientAddressException, ConnectionTypeException,
            UserNameException, LoginException;

    long getLastActivity();

    void updateLastActivity();

    void updateLastKeepAlive();

    long getLastKeepAlive();

    boolean isLoggedIn();

    KailleraServer getServer();

    KailleraEventListener getListener();

    void chat(String message) throws ChatException, FloodException;

    KailleraGame createGame(String romName) throws CreateGameException, FloodException;

    void quit(String message)
            throws QuitException, DropGameException, QuitGameException, CloseGameException;

    KailleraGame joinGame(int gameID) throws JoinGameException;

    int getPlayerNumber();

    void startGame() throws StartGameException;

    void gameChat(String message, int messageID) throws GameChatException;

    void gameKick(int userID) throws GameKickException;

    void playerReady() throws UserReadyException;

    void addGameData(byte[] data) throws GameDataException;

    void dropGame() throws DropGameException;

    void quitGame() throws DropGameException, QuitGameException, CloseGameException;

    void droppedPacket();

    void stop();
}
