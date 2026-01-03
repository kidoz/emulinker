package su.kidoz.kaillera.service;

import su.kidoz.kaillera.access.AccessManager;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.exception.ChatException;
import su.kidoz.kaillera.model.exception.FloodException;
import su.kidoz.kaillera.model.impl.KailleraUserImpl;
import su.kidoz.util.EmuLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for validating chat messages and enforcing chat moderation rules.
 * Handles silencing, flood control, and illegal character detection.
 */
public class ChatModerationService {
    private static final Logger log = LoggerFactory.getLogger(ChatModerationService.class);

    private final AccessManager accessManager;
    private final int chatFloodTime;
    private final int maxChatLength;

    public ChatModerationService(final AccessManager accessManager, final int chatFloodTime,
            final int maxChatLength) {
        this.accessManager = accessManager;
        this.chatFloodTime = chatFloodTime;
        this.maxChatLength = maxChatLength;
    }

    /**
     * Validates that the user is allowed to chat and the message is valid.
     *
     * @param user
     *            the user sending the message
     * @param message
     *            the chat message
     * @return the trimmed and validated message
     * @throws ChatException
     *             if chat is denied
     * @throws FloodException
     *             if chat flood control triggered
     */
    public String validateChat(final KailleraUser user, final String message)
            throws ChatException, FloodException {
        if (!user.isLoggedIn()) {
            log.error(user + " chat failed: Not logged in");
            throw new ChatException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
        }

        final int access = accessManager.getAccess(user.getSocketAddress().getAddress());

        // Check if user is silenced
        if (access == AccessManager.ACCESS_NORMAL
                && accessManager.isSilenced(user.getSocketAddress().getAddress())) {
            log.warn(user + " chat denied: Silenced: " + message);
            throw new ChatException(EmuLang.getString("KailleraServerImpl.ChatDeniedSilenced"));
        }

        // Check flood control
        if (access == AccessManager.ACCESS_NORMAL && chatFloodTime > 0) {
            final long lastChatTime = ((KailleraUserImpl) user).getLastChatTime();
            if ((System.currentTimeMillis() - lastChatTime) < (chatFloodTime * 1000L)) {
                log.warn(user + " chat denied: Flood: " + message);
                throw new FloodException(
                        EmuLang.getString("KailleraServerImpl.ChatDeniedFloodControl"));
            }
        }

        final String trimmedMessage = message.trim();
        if (trimmedMessage.isEmpty()) {
            return ""; // Empty messages are silently ignored
        }

        // Validate message content for normal users
        if (access == AccessManager.ACCESS_NORMAL) {
            if (containsIllegalCharacters(trimmedMessage)) {
                log.warn(user + " chat denied: Illegal characters in message");
                throw new ChatException(
                        EmuLang.getString("KailleraServerImpl.ChatDeniedIllegalCharacters"));
            }

            if (maxChatLength > 0 && trimmedMessage.length() > maxChatLength) {
                log.warn(user + " chat denied: Message Length > " + maxChatLength);
                throw new ChatException(
                        EmuLang.getString("KailleraServerImpl.ChatDeniedMessageTooLong"));
            }
        }

        return trimmedMessage;
    }

    /**
     * Checks if a string contains illegal characters (control chars, dangerous
     * Unicode).
     *
     * @param str
     *            the string to check
     * @return true if illegal characters are found
     */
    private boolean containsIllegalCharacters(final String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            // Check for control characters (< 32), DEL (127), and other ISO control chars
            if (c < 32 || c == 127 || Character.isISOControl(c)) {
                return true;
            }
            // Check for Unicode direction override characters (potential spoofing)
            if (c == '\u202A' || c == '\u202B' || c == '\u202C' || c == '\u202D' || c == '\u202E'
                    || c == '\u2066' || c == '\u2067' || c == '\u2068' || c == '\u2069') {
                return true;
            }
            // Check for zero-width characters (potential spoofing)
            if (c == '\u200B' || c == '\u200C' || c == '\u200D' || c == '\uFEFF') {
                return true;
            }
        }
        return false;
    }
}
