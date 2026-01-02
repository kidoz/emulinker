package su.kidoz.kaillera.model.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.model.impl.KailleraUserImpl;
import org.emulinker.util.EmuLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles periodic server maintenance tasks such as timeout checking, ban
 * enforcement, and user cleanup. Extracted from KailleraServerImpl.run() to
 * improve separation of concerns.
 */
public class ServerMaintenanceTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServerMaintenanceTask.class);

    private final UserManager userManager;
    private final AccessManager accessManager;
    private final int maxPing;
    private final int keepAliveTimeout;
    private final int idleTimeout;
    private final Consumer<UserQuitRequest> quitHandler;

    private volatile boolean stopFlag = false;

    /**
     * Request to quit a user with a specific message.
     */
    public record UserQuitRequest(KailleraUserImpl user, String message) {
    }

    public ServerMaintenanceTask(UserManager userManager, AccessManager accessManager, int maxPing,
            int keepAliveTimeout, int idleTimeout, Consumer<UserQuitRequest> quitHandler) {
        this.userManager = userManager;
        this.accessManager = accessManager;
        this.maxPing = maxPing;
        this.keepAliveTimeout = keepAliveTimeout;
        this.idleTimeout = idleTimeout;
        this.quitHandler = quitHandler;
    }

    public void stop() {
        stopFlag = true;
    }

    public boolean isStopped() {
        return stopFlag;
    }

    @Override
    public void run() {
        log.debug("ServerMaintenanceTask thread running...");

        try {
            while (!stopFlag) {
                try {
                    Thread.sleep((long) (maxPing * 3));
                } catch (InterruptedException e) {
                    log.error("Sleep Interrupted!", e);
                }

                if (stopFlag) {
                    break;
                }

                if (userManager.isEmpty()) {
                    continue;
                }

                performMaintenance();
            }
        } catch (Throwable e) {
            if (!stopFlag) {
                log.error("ServerMaintenanceTask thread caught unexpected exception: " + e, e);
            }
        } finally {
            log.debug("ServerMaintenanceTask thread exiting...");
        }
    }

    private void performMaintenance() {
        // Copy to avoid ConcurrentModificationException when removing during iteration
        List<KailleraUserImpl> usersSnapshot = new ArrayList<>(userManager.getUsers());
        List<Integer> usersToRemove = new ArrayList<>();

        for (KailleraUserImpl user : usersSnapshot) {
            synchronized (user) {
                int access = accessManager.getAccess(user.getConnectSocketAddress().getAddress());
                user.setAccess(access);

                MaintenanceAction action = checkUser(user, access);
                switch (action) {
                    case REMOVE_TIMEOUT :
                        log.info(user + " connection timeout!");
                        user.stop();
                        usersToRemove.add(user.getID());
                        break;
                    case QUIT_KEEPALIVE :
                        log.info(user + " keepalive timeout!");
                        requestQuit(user,
                                EmuLang.getString("KailleraServerImpl.ForcedQuitPingTimeout"));
                        break;
                    case QUIT_IDLE :
                        log.info(user + " inactivity timeout!");
                        requestQuit(user, EmuLang
                                .getString("KailleraServerImpl.ForcedQuitInactivityTimeout"));
                        break;
                    case QUIT_BANNED :
                        log.info(user + " banned!");
                        requestQuit(user, EmuLang.getString("KailleraServerImpl.ForcedQuitBanned"));
                        break;
                    case QUIT_EMULATOR_RESTRICTED :
                        log.info(user + ": emulator restricted!");
                        requestQuit(user, EmuLang
                                .getString("KailleraServerImpl.ForcedQuitEmulatorRestricted"));
                        break;
                    case NONE :
                    default :
                        break;
                }
            }
        }

        // Remove users after iteration to avoid ConcurrentModificationException
        for (Integer userId : usersToRemove) {
            userManager.removeUser(userId);
        }
    }

    private MaintenanceAction checkUser(KailleraUserImpl user, int access) {
        long now = System.currentTimeMillis();

        // Connection timeout (not logged in yet)
        if (!user.isLoggedIn() && (now - user.getConnectTime()) > (maxPing * 15)) {
            return MaintenanceAction.REMOVE_TIMEOUT;
        }

        // Keepalive timeout
        if (user.isLoggedIn() && (now - user.getLastKeepAlive()) > (keepAliveTimeout * 1000L)) {
            return MaintenanceAction.QUIT_KEEPALIVE;
        }

        // Idle timeout (only for normal users)
        if (idleTimeout > 0 && access == AccessManager.ACCESS_NORMAL && user.isLoggedIn()
                && (now - user.getLastActivity()) > (idleTimeout * 1000L)) {
            return MaintenanceAction.QUIT_IDLE;
        }

        // Banned
        if (user.isLoggedIn() && access < AccessManager.ACCESS_NORMAL) {
            return MaintenanceAction.QUIT_BANNED;
        }

        // Emulator restricted
        if (user.isLoggedIn() && access == AccessManager.ACCESS_NORMAL
                && !accessManager.isEmulatorAllowed(user.getClientType())) {
            return MaintenanceAction.QUIT_EMULATOR_RESTRICTED;
        }

        return MaintenanceAction.NONE;
    }

    private void requestQuit(KailleraUserImpl user, String message) {
        try {
            quitHandler.accept(new UserQuitRequest(user, message));
        } catch (Exception e) {
            log.error("Error forcing " + user + " quit: " + message, e);
        }
    }

    private enum MaintenanceAction {
        NONE, REMOVE_TIMEOUT, QUIT_KEEPALIVE, QUIT_IDLE, QUIT_BANNED, QUIT_EMULATOR_RESTRICTED
    }
}
