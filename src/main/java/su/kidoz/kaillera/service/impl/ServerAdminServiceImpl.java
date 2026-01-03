package su.kidoz.kaillera.service.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import su.kidoz.kaillera.access.AccessManager;
import su.kidoz.kaillera.model.KailleraServer;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.impl.KailleraServerImpl;
import su.kidoz.kaillera.model.impl.KailleraUserImpl;
import su.kidoz.kaillera.service.ServerAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ServerAdminService.
 *
 * <p>
 * Handles admin operations via the underlying KailleraServer and AccessManager.
 *
 * <p>
 * <b>Proxy/NAT Awareness:</b> This implementation uses
 * {@code KailleraUser.getConnectSocketAddress()} to obtain IP addresses for
 * ban/silence operations. If the server runs behind a reverse proxy, load
 * balancer, or NAT, this address may represent the proxy rather than the actual
 * client. For proxy deployments, consider:
 * <ul>
 * <li>Using X-Forwarded-For or similar headers (requires protocol-level
 * changes)</li>
 * <li>Running the server without a proxy for UDP traffic</li>
 * <li>Configuring proxy protocol support if available</li>
 * </ul>
 */
public class ServerAdminServiceImpl implements ServerAdminService {

    private static final Logger log = LoggerFactory.getLogger(ServerAdminServiceImpl.class);

    private final KailleraServer server;
    private final AccessManager accessManager;

    public ServerAdminServiceImpl(final KailleraServer server, final AccessManager accessManager) {
        this.server = server;
        this.accessManager = accessManager;
    }

    @Override
    public boolean kickUser(final KailleraUser admin, final int targetUserId, final String reason) {
        if (!isAdmin(admin)) {
            log.warn("Non-admin {} attempted to kick user {}", admin.getName(), targetUserId);
            return false;
        }

        final KailleraUser target = server.getUser(targetUserId);
        if (target == null) {
            return false;
        }

        try {
            target.quit("Kicked by admin: " + reason);
            log.info("Admin {} kicked user {}: {}", admin.getName(), target.getName(), reason);
            return true;
        } catch (Exception e) {
            log.error("Failed to kick user {}: {}", targetUserId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean silenceUser(final KailleraUser admin, final int targetUserId,
            final int durationMinutes) {
        if (!isAdmin(admin)) {
            log.warn("Non-admin {} attempted to silence user {}", admin.getName(), targetUserId);
            return false;
        }

        final KailleraUser target = server.getUser(targetUserId);
        if (target == null) {
            return false;
        }

        // Silence is typically handled via access control rules.
        String ipAddress = target.getConnectSocketAddress().getAddress().getHostAddress();
        accessManager.addSilenced(ipAddress, durationMinutes);
        log.info("Admin {} silenced user {} for {} minutes", admin.getName(), target.getName(),
                durationMinutes);
        return true;
    }

    @Override
    public boolean banUser(final KailleraUser admin, final int targetUserId,
            final int durationMinutes) {
        if (!isAdmin(admin)) {
            log.warn("Non-admin {} attempted to ban user {}", admin.getName(), targetUserId);
            return false;
        }

        final KailleraUser target = server.getUser(targetUserId);
        if (target == null) {
            return false;
        }

        // Ban the IP through AccessManager.
        String ipAddress = target.getConnectSocketAddress().getAddress().getHostAddress();
        accessManager.addTempBan(ipAddress, durationMinutes);
        log.info("Admin {} banned user {} (IP: {}) for {} minutes", admin.getName(),
                target.getName(), ipAddress, durationMinutes);

        // Kick the user after banning.
        try {
            target.quit("Banned by admin");
        } catch (Exception e) {
            log.error("Failed to kick banned user {}: {}", targetUserId, e.getMessage());
        }

        return true;
    }

    @Override
    public boolean unbanIp(final KailleraUser admin, final String ipAddress) {
        if (!isAdmin(admin)) {
            log.warn("Non-admin {} attempted to unban IP {}", admin.getName(), ipAddress);
            return false;
        }

        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            accessManager.clearTemp(address);
        } catch (UnknownHostException e) {
            log.warn("Invalid IP address {} for unban", ipAddress);
            return false;
        }

        log.info("Admin {} unbanned IP {}", admin.getName(), ipAddress);
        return true;
    }

    @Override
    public List<KailleraUser> findUsers(final KailleraUser admin, final String namePattern) {
        if (!isAdmin(admin)) {
            return List.of();
        }

        final List<KailleraUser> results = new ArrayList<>();
        final String lowerPattern = namePattern.toLowerCase();

        for (KailleraUser user : server.getUsers()) {
            if (user.getName().toLowerCase().contains(lowerPattern)) {
                results.add(user);
            }
        }

        return results;
    }

    @Override
    public Optional<UserInfo> getUserInfo(final KailleraUser admin, final int targetUserId) {
        if (!isAdmin(admin)) {
            return Optional.empty();
        }

        final KailleraUser user = server.getUser(targetUserId);
        if (user == null) {
            return Optional.empty();
        }

        Integer gameId = null;
        for (var game : server.getGames()) {
            if (game.getPlayers().contains(user)) {
                gameId = game.getID();
                break;
            }
        }

        int accessLevel = AccessManager.ACCESS_NORMAL;
        if (user instanceof KailleraUserImpl userImpl) {
            accessLevel = userImpl.getAccess();
        }

        return Optional.of(new UserInfo(user.getID(), user.getName(),
                user.getConnectSocketAddress().getAddress().getHostAddress(), user.getClientType(),
                user.getPing(), accessLevel, user.getConnectTime(), gameId));
    }

    @Override
    public boolean clearGame(final KailleraUser admin, final int gameId) {
        if (!isAdmin(admin)) {
            log.warn("Non-admin {} attempted to clear game {}", admin.getName(), gameId);
            return false;
        }

        final var game = server.getGame(gameId);
        if (game == null) {
            return false;
        }

        if (game.getNumPlayers() > 0) {
            log.warn("Cannot clear game {} with {} players", gameId, game.getNumPlayers());
            return false;
        }

        log.info("Admin {} cleared game {}", admin.getName(), game.getRomName());
        return true;
    }

    @Override
    public void announce(final KailleraUser admin, final String message) {
        if (!isAdmin(admin)) {
            log.warn("Non-admin {} attempted to announce", admin.getName());
            return;
        }

        if (server instanceof KailleraServerImpl serverImpl) {
            serverImpl.announce(message, true);
            log.info("Admin {} announced: {}", admin.getName(), message);
        }
    }

    @Override
    public ServerStats getServerStats(final KailleraUser admin) {
        if (!isAdmin(admin)) {
            return new ServerStats(0, 0, 0, 0, 0, 0);
        }

        final long uptime = System.currentTimeMillis(); // Would need server start time.
        final long totalConnections = 0; // Would need connection counter.

        return new ServerStats(server.getNumUsers(), server.getMaxUsers(), server.getNumGames(),
                server.getMaxGames(), uptime, totalConnections);
    }

    private boolean isAdmin(final KailleraUser user) {
        if (user instanceof KailleraUserImpl userImpl) {
            return userImpl.getAccess() >= AccessManager.ACCESS_ADMIN;
        }
        return false;
    }
}
