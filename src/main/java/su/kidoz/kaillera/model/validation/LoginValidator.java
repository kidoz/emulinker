package su.kidoz.kaillera.model.validation;

import org.emulinker.config.ServerConfig;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.exception.ClientAddressException;
import org.emulinker.kaillera.model.exception.LoginException;
import org.emulinker.kaillera.model.exception.PingTimeException;
import org.emulinker.kaillera.model.exception.UserNameException;
import org.emulinker.kaillera.model.impl.KailleraUserImpl;
import org.emulinker.util.EmuLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates user login requests. Extracts validation logic from
 * KailleraServerImpl to improve testability and separation of concerns.
 */
public class LoginValidator {
    private static final Logger log = LoggerFactory.getLogger(LoginValidator.class);

    private final AccessManager accessManager;
    private final ServerConfig serverConfig;
    private final int maxPing;
    private final int maxUserNameLength;
    private final int maxClientNameLength;
    private final boolean allowMultipleConnections;

    public LoginValidator(AccessManager accessManager, ServerConfig serverConfig) {
        this.accessManager = accessManager;
        this.serverConfig = serverConfig;
        this.maxPing = serverConfig.getMaxPing();
        this.maxUserNameLength = serverConfig.getMaxUserNameLength();
        this.maxClientNameLength = serverConfig.getMaxClientNameLength();
        this.allowMultipleConnections = serverConfig.isAllowMultipleConnections();
    }

    /**
     * Validates that the user is not already logged in.
     */
    public void validateNotAlreadyLoggedIn(KailleraUser user) throws LoginException {
        if (user.isLoggedIn()) {
            log.warn("{} login denied: Already logged in!", user);
            throw new LoginException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedAlreadyLoggedIn"));
        }
    }

    /**
     * Validates that the user exists in the server's user list.
     */
    public void validateUserExists(KailleraUser user, KailleraUser userFromList)
            throws LoginException {
        if (userFromList == null) {
            log.warn("{} login denied: Connection timed out!", user);
            throw new LoginException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedConnectionTimedOut"));
        }
    }

    /**
     * Validates access level (not banned).
     *
     * @return the access level
     */
    public int validateAccessLevel(KailleraUser user) throws LoginException {
        int access = accessManager.getAccess(user.getSocketAddress().getAddress());
        if (access < AccessManager.ACCESS_NORMAL) {
            log.info("{} login denied: Access denied", user);
            throw new LoginException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedAccessDenied"));
        }
        return access;
    }

    /**
     * Validates ping is within limits (for normal users).
     */
    public void validatePing(KailleraUser user, int access) throws PingTimeException {
        if (access == AccessManager.ACCESS_NORMAL && maxPing > 0 && user.getPing() > maxPing) {
            log.info("{} login denied: Ping {} > {}", user, user.getPing(), maxPing);
            throw new PingTimeException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedPingTooHigh",
                            (user.getPing() + " > " + maxPing)));
        }

        if (user.getPing() < 0) {
            log.warn("{} login denied: Invalid ping: {}", user, user.getPing());
            throw new PingTimeException(
                    EmuLang.getString("KailleraServerImpl.LoginErrorInvalidPing", user.getPing()));
        }
    }

    /**
     * Validates connection type is allowed.
     */
    public void validateConnectionType(KailleraUser user, int access) throws LoginException {
        if (access == AccessManager.ACCESS_NORMAL
                && !serverConfig.isConnectionTypeAllowed(user.getConnectionType())) {
            log.info("{} login denied: Connection {} Not Allowed", user,
                    KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]);
            throw new LoginException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedConnectionTypeDenied",
                            KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]));
        }
    }

    /**
     * Validates username (not empty, within length limits, no illegal characters).
     */
    public void validateUserName(KailleraUser user, int access) throws UserNameException {
        if (access == AccessManager.ACCESS_NORMAL && user.getName().trim().isEmpty()) {
            log.info("{} login denied: Empty UserName", user);
            throw new UserNameException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedUserNameEmpty"));
        }

        if (access == AccessManager.ACCESS_NORMAL && maxUserNameLength > 0
                && user.getName().length() > maxUserNameLength) {
            log.info("{} login denied: UserName Length > {}", user, maxUserNameLength);
            throw new UserNameException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedUserNameTooLong"));
        }

        if (access == AccessManager.ACCESS_NORMAL && containsIllegalCharacters(user.getName())) {
            log.info("{} login denied: Illegal characters in UserName", user);
            throw new UserNameException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedIllegalCharactersInUserName"));
        }
    }

    /**
     * Validates client/emulator name length.
     */
    public void validateClientName(KailleraUser user, int access) throws UserNameException {
        if (access == AccessManager.ACCESS_NORMAL && maxClientNameLength > 0
                && user.getClientType().length() > maxClientNameLength) {
            log.info("{} login denied: Client Name Length > {}", user, maxClientNameLength);
            throw new UserNameException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedEmulatorNameTooLong"));
        }
    }

    /**
     * Validates user status is CONNECTING.
     */
    public void validateUserStatus(KailleraUser user, KailleraUser userFromList)
            throws LoginException {
        if (userFromList.getStatus() != KailleraUser.STATUS_CONNECTING) {
            log.warn("{} login denied: Invalid status={}", user,
                    KailleraUser.STATUS_NAMES[userFromList.getStatus()]);
            throw new LoginException(EmuLang.getString("KailleraServerImpl.LoginErrorInvalidStatus",
                    userFromList.getStatus()));
        }
    }

    /**
     * Validates that connect address matches login address.
     */
    public void validateAddressMatch(KailleraUser user, KailleraUser userFromList)
            throws ClientAddressException {
        if (!userFromList.getConnectSocketAddress().getAddress()
                .equals(user.getSocketAddress().getAddress())) {
            log.warn("{} login denied: Connect address does not match login address: {} != {}",
                    user, userFromList.getConnectSocketAddress().getAddress().getHostAddress(),
                    user.getSocketAddress().getAddress().getHostAddress());
            throw new ClientAddressException(
                    EmuLang.getString("KailleraServerImpl.LoginDeniedAddressMatchError"));
        }
    }

    /**
     * Validates that the emulator is allowed.
     */
    public void validateEmulator(KailleraUser user, int access) throws LoginException {
        if (access == AccessManager.ACCESS_NORMAL
                && !accessManager.isEmulatorAllowed(user.getClientType())) {
            log.info("{} login denied: AccessManager denied emulator: {}", user,
                    user.getClientType());
            throw new LoginException(EmuLang.getString(
                    "KailleraServerImpl.LoginDeniedEmulatorRestricted", user.getClientType()));
        }
    }

    /**
     * Checks if multiple connections from the same address should be denied.
     *
     * @return the existing user if this is a reconnect attempt (same name and
     *         address), null otherwise
     */
    public KailleraUserImpl checkDuplicateLogin(KailleraUser user, KailleraUser userFromList,
            Iterable<KailleraUserImpl> allUsers, int access) throws ClientAddressException {
        KailleraUserImpl reconnectUser = null;

        for (KailleraUserImpl existingUser : allUsers) {
            if (!existingUser.isLoggedIn()) {
                continue;
            }

            boolean sameAddress = existingUser.getConnectSocketAddress().getAddress()
                    .equals(userFromList.getConnectSocketAddress().getAddress());
            boolean sameName = user.getName().equals(existingUser.getName());
            boolean differentUser = !existingUser.equals(userFromList);

            if (differentUser && sameAddress && sameName) {
                // User reconnecting with same name and address - mark for forced quit
                reconnectUser = existingUser;
            } else if (access == AccessManager.ACCESS_NORMAL && differentUser && sameAddress
                    && !sameName && !allowMultipleConnections) {
                log.warn("{} login denied: Address already logged in as {}", user,
                        existingUser.getName());
                throw new ClientAddressException(EmuLang.getString(
                        "KailleraServerImpl.LoginDeniedAlreadyLoggedInAs", existingUser.getName()));
            }
        }

        return reconnectUser;
    }

    /**
     * Checks if a string contains illegal characters (control chars, dangerous
     * Unicode).
     */
    private boolean containsIllegalCharacters(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < 32 || c == 127 || Character.isISOControl(c)) {
                return true;
            }
            // Unicode direction override characters
            if (c == '\u202A' || c == '\u202B' || c == '\u202C' || c == '\u202D' || c == '\u202E'
                    || c == '\u2066' || c == '\u2067' || c == '\u2068' || c == '\u2069') {
                return true;
            }
            // Zero-width characters
            if (c == '\u200B' || c == '\u200C' || c == '\u200D' || c == '\uFEFF') {
                return true;
            }
        }
        return false;
    }
}
