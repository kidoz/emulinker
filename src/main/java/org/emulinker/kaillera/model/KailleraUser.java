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

/**
 * Represents a connected user in the Kaillera server. Manages user state,
 * profile information, and provides methods for server and game interactions.
 *
 * <p>
 * Users transition through several states:
 * <ul>
 * <li>{@link #STATUS_CONNECTING} - Initial connection, awaiting login</li>
 * <li>{@link #STATUS_IDLE} - Logged in, in server lobby</li>
 * <li>{@link #STATUS_PLAYING} - Participating in a game</li>
 * </ul>
 *
 * <p>
 * Connection types indicate the user's self-reported network quality, which
 * affects game synchronization behavior.
 *
 * @see KailleraServer
 * @see KailleraGame
 */
public interface KailleraUser {
    /** LAN connection - lowest latency, best for local network play. */
    byte CONNECTION_TYPE_LAN = 1;
    /** Excellent internet connection. */
    byte CONNECTION_TYPE_EXCELLENT = 2;
    /** Good internet connection. */
    byte CONNECTION_TYPE_GOOD = 3;
    /** Average internet connection. */
    byte CONNECTION_TYPE_AVERAGE = 4;
    /** Low quality internet connection. */
    byte CONNECTION_TYPE_LOW = 5;
    /** Bad internet connection - highest latency tolerance. */
    byte CONNECTION_TYPE_BAD = 6;

    /** Human-readable names for connection types indexed by type value. */
    String[] CONNECTION_TYPE_NAMES = {"DISABLED", "Lan", "Excellent", "Good", "Average", "Low",
            "Bad"};

    /** User is currently playing in a game. */
    byte STATUS_PLAYING = 0;
    /** User is in the server lobby, not in a game. */
    byte STATUS_IDLE = 1;
    /** User is connecting but has not completed login. */
    byte STATUS_CONNECTING = 2;
    /** Human-readable names for status values indexed by status value. */
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
