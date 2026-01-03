package su.kidoz.kaillera.service;

import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.exception.ChatException;
import su.kidoz.kaillera.model.exception.FloodException;
import su.kidoz.kaillera.model.exception.GameChatException;

/**
 * Service interface for communication operations (chat, announcements).
 * Separates messaging from the main server implementation.
 */
public interface CommunicationService {

    /**
     * Sends a server-wide chat message.
     *
     * @param user
     *            the user sending the message
     * @param message
     *            the chat message
     * @throws ChatException
     *             if the chat fails
     * @throws FloodException
     *             if the user is chatting too frequently
     */
    void serverChat(KailleraUser user, String message) throws ChatException, FloodException;

    /**
     * Sends a game chat message.
     *
     * @param user
     *            the user sending the message
     * @param message
     *            the chat message
     * @param messageId
     *            the message ID
     * @throws GameChatException
     *             if the chat fails
     */
    void gameChat(KailleraUser user, String message, int messageId) throws GameChatException;

    /**
     * Sends a server-wide announcement.
     *
     * @param message
     *            the announcement message
     * @param includeGames
     *            if true, announce to users in games as well
     */
    void announce(String message, boolean includeGames);

    /**
     * Sends a private message to a specific user.
     *
     * @param from
     *            the user sending the message
     * @param toUserId
     *            the target user ID
     * @param message
     *            the message
     * @return true if the message was sent, false if target user not found
     */
    boolean privateMessage(KailleraUser from, int toUserId, String message);
}
