package su.kidoz.kaillera.service;

import java.util.List;
import java.util.Optional;

import su.kidoz.kaillera.model.KailleraUser;

/**
 * Service layer for server administration operations.
 *
 * <p>
 * Handles admin commands like ban, kick, silence, and user lookup.
 */
public interface ServerAdminService {

    /**
     * Kicks a user from the server.
     *
     * @param admin
     *            the admin performing the action
     * @param targetUserId
     *            the user ID to kick
     * @param reason
     *            the kick reason
     * @return true if the user was kicked
     */
    boolean kickUser(KailleraUser admin, int targetUserId, String reason);

    /**
     * Silences a user (prevents chat).
     *
     * @param admin
     *            the admin performing the action
     * @param targetUserId
     *            the user ID to silence
     * @param durationMinutes
     *            duration in minutes (0 for permanent)
     * @return true if the user was silenced
     */
    boolean silenceUser(KailleraUser admin, int targetUserId, int durationMinutes);

    /**
     * Bans a user by IP address.
     *
     * @param admin
     *            the admin performing the action
     * @param targetUserId
     *            the user ID to ban
     * @param durationMinutes
     *            duration in minutes (0 for permanent)
     * @return true if the ban was applied
     */
    boolean banUser(KailleraUser admin, int targetUserId, int durationMinutes);

    /**
     * Unbans an IP address.
     *
     * @param admin
     *            the admin performing the action
     * @param ipAddress
     *            the IP address to unban
     * @return true if the IP was unbanned
     */
    boolean unbanIp(KailleraUser admin, String ipAddress);

    /**
     * Finds users by partial name match.
     *
     * @param admin
     *            the admin performing the search
     * @param namePattern
     *            the name pattern to search for
     * @return list of matching users
     */
    List<KailleraUser> findUsers(KailleraUser admin, String namePattern);

    /**
     * Gets detailed information about a user.
     *
     * @param admin
     *            the admin requesting info
     * @param targetUserId
     *            the user ID
     * @return user information, or empty if not found
     */
    Optional<UserInfo> getUserInfo(KailleraUser admin, int targetUserId);

    /**
     * Clears an empty game from the server.
     *
     * @param admin
     *            the admin performing the action
     * @param gameId
     *            the game ID to clear
     * @return true if the game was cleared
     */
    boolean clearGame(KailleraUser admin, int gameId);

    /**
     * Sends an announcement to all users.
     *
     * @param admin
     *            the admin sending the announcement
     * @param message
     *            the announcement message
     */
    void announce(KailleraUser admin, String message);

    /**
     * Gets server statistics.
     *
     * @param admin
     *            the admin requesting stats
     * @return server statistics
     */
    ServerStats getServerStats(KailleraUser admin);

    /** Information about a user. */
    record UserInfo(int id, String name, String ipAddress, String clientType, int ping,
            int accessLevel, long connectedSince, Integer gameId) {
    }

    /** Server statistics. */
    record ServerStats(int userCount, int maxUsers, int gameCount, int maxGames, long uptime,
            long totalConnections) {
    }
}
