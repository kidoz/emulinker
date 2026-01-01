package org.emulinker.kaillera.service;

import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.exception.GameDataException;
import org.emulinker.kaillera.model.exception.GameKickException;
import org.emulinker.kaillera.model.exception.UserReadyException;

/**
 * Service layer for game input operations.
 *
 * <p>
 * Handles frame data submission, player ready signals, and in-game player
 * management.
 */
public interface GameInputService {

    /**
     * Submits game frame data from a player.
     *
     * @param user
     *            the user submitting data
     * @param data
     *            the frame data
     * @throws GameDataException
     *             if the data submission fails
     */
    void submitFrameData(KailleraUser user, byte[] data) throws GameDataException;

    /**
     * Marks a player as ready to start the game.
     *
     * @param user
     *            the user signaling ready
     * @throws UserReadyException
     *             if the ready signal fails
     */
    void markPlayerReady(KailleraUser user) throws UserReadyException;

    /**
     * Kicks a player from the current game.
     *
     * @param requester
     *            the user requesting the kick (must be game owner)
     * @param targetUserId
     *            the ID of the user to kick
     * @throws GameKickException
     *             if the kick operation fails
     */
    void kickPlayer(KailleraUser requester, int targetUserId) throws GameKickException;

    /**
     * Reports a dropped packet from a user.
     *
     * @param user
     *            the user who dropped a packet
     */
    void reportDroppedPacket(KailleraUser user);

    /**
     * Configures autofire settings for a game.
     *
     * @param user
     *            the game owner
     * @param autofireValue
     *            the autofire delay value
     * @return true if autofire was configured successfully
     */
    boolean configureAutofire(KailleraUser user, int autofireValue);
}
