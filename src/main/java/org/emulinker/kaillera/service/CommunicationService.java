package org.emulinker.kaillera.service;

import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.exception.ChatException;
import org.emulinker.kaillera.model.exception.FloodException;
import org.emulinker.kaillera.model.exception.GameChatException;

/**
 * Service layer for communication operations.
 *
 * <p>
 * Handles server-wide chat, game chat, and announcements.
 */
public interface CommunicationService {

    /**
     * Sends a chat message to the server lobby.
     *
     * @param user
     *            the user sending the message
     * @param message
     *            the chat message
     * @throws ChatException
     *             if the chat operation fails
     * @throws FloodException
     *             if the user is chatting too fast
     */
    void serverChat(KailleraUser user, String message) throws ChatException, FloodException;

    /**
     * Sends a chat message within a game.
     *
     * @param user
     *            the user sending the message
     * @param message
     *            the chat message
     * @param messageId
     *            the message ID for ordering
     * @throws GameChatException
     *             if the game chat operation fails
     */
    void gameChat(KailleraUser user, String message, int messageId) throws GameChatException;

    /**
     * Broadcasts an announcement to all users.
     *
     * @param message
     *            the announcement message
     * @param includeGames
     *            whether to also broadcast to users in games
     */
    void announce(String message, boolean includeGames);

    /**
     * Sends a private message to a specific user.
     *
     * @param from
     *            the sender
     * @param toUserId
     *            the recipient user ID
     * @param message
     *            the message content
     * @return true if the message was delivered
     */
    boolean privateMessage(KailleraUser from, int toUserId, String message);
}
