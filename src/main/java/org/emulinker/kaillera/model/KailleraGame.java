package org.emulinker.kaillera.model;

import java.util.Collection;

import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.impl.AutoFireDetector;
import org.emulinker.kaillera.model.exception.CloseGameException;
import org.emulinker.kaillera.model.exception.DropGameException;
import org.emulinker.kaillera.model.exception.GameChatException;
import org.emulinker.kaillera.model.exception.GameDataException;
import org.emulinker.kaillera.model.exception.GameKickException;
import org.emulinker.kaillera.model.exception.JoinGameException;
import org.emulinker.kaillera.model.exception.QuitGameException;
import org.emulinker.kaillera.model.exception.StartGameException;
import org.emulinker.kaillera.model.exception.UserReadyException;

/**
 * Represents a game session in the Kaillera server. Manages players, game
 * state, and synchronizes emulator input data across all participants.
 *
 * <p>
 * Games transition through the following states:
 * <ul>
 * <li>{@link #STATUS_WAITING} - Waiting for players to join and ready up</li>
 * <li>{@link #STATUS_SYNCHRONIZING} - All players ready, synchronizing
 * start</li>
 * <li>{@link #STATUS_PLAYING} - Game in progress, exchanging input data</li>
 * </ul>
 *
 * <p>
 * The game owner (creator) has special privileges including the ability to kick
 * players and start the game. When the owner leaves, the game is closed.
 *
 * @see KailleraServer
 * @see KailleraUser
 */
public interface KailleraGame {
    /** Game is waiting for players to join and ready up. */
    byte STATUS_WAITING = 0;
    /** Game is in progress, actively exchanging input data. */
    byte STATUS_PLAYING = 1;
    /** Game is synchronizing player starts. */
    byte STATUS_SYNCHRONIZING = 2;

    /** Human-readable names for game status values. */
    String[] STATUS_NAMES = {"Waiting", "Playing", "Synchronizing"};

    int getID();

    String getRomName();

    String getClientType();

    KailleraUser getOwner();

    int getPlayerNumber(KailleraUser user);

    int getNumPlayers();

    KailleraUser getPlayer(int playerNumber);

    Collection<? extends KailleraUser> getPlayers();

    int getStatus();

    KailleraServer getServer();

    void droppedPacket(KailleraUser user);

    int join(KailleraUser user) throws JoinGameException;

    void chat(KailleraUser user, String message) throws GameChatException;

    void kick(KailleraUser requester, int userID) throws GameKickException;

    void start(KailleraUser user) throws StartGameException;

    void ready(KailleraUser user, int playerNumber) throws UserReadyException;

    void addData(KailleraUser user, int playerNumber, byte[] data) throws GameDataException;

    void drop(KailleraUser user, int playerNumber) throws DropGameException;

    void quit(KailleraUser user, int playerNumber)
            throws DropGameException, QuitGameException, CloseGameException;

    /**
     * Broadcasts an announcement message to all players in this game.
     *
     * @param message
     *            the announcement text
     */
    void announce(String message);

    /**
     * Returns the auto-fire detector for this game.
     *
     * @return the auto-fire detector, or null if detection is disabled
     */
    AutoFireDetector getAutoFireDetector();

    /**
     * Adds a game event to be broadcast to all player listeners.
     *
     * @param event
     *            the game event to broadcast
     */
    void addEvent(GameEvent event);
}
