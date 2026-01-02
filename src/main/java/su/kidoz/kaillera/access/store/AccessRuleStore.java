package su.kidoz.kaillera.access.store;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.emulinker.kaillera.access.AccessManager;

import su.kidoz.kaillera.access.parser.ParseResult;
import su.kidoz.kaillera.access.rule.AddressRule;
import su.kidoz.kaillera.access.rule.NameRule;

/**
 * Thread-safe store for permanent access rules loaded from configuration. Uses
 * CopyOnWriteArrayList for safe concurrent reads (which are much more frequent
 * than writes/reloads).
 */
public class AccessRuleStore {

    private final CopyOnWriteArrayList<AddressRule> userRules = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<AddressRule> addressRules = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<NameRule> gameRules = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<NameRule> emulatorRules = new CopyOnWriteArrayList<>();

    /**
     * Updates all rules from a parse result. Clears existing rules and replaces
     * them atomically.
     *
     * @param result
     *            the parsed rules
     */
    public synchronized void update(ParseResult result) {
        userRules.clear();
        addressRules.clear();
        gameRules.clear();
        emulatorRules.clear();

        userRules.addAll(result.userRules());
        addressRules.addAll(result.addressRules());
        gameRules.addAll(result.gameRules());
        emulatorRules.addAll(result.emulatorRules());
    }

    /**
     * Clears all rules.
     */
    public synchronized void clear() {
        userRules.clear();
        addressRules.clear();
        gameRules.clear();
        emulatorRules.clear();
    }

    /**
     * Refreshes DNS resolutions for all rules that have DNS entries.
     */
    public void refreshDns() {
        for (AddressRule rule : userRules) {
            if (rule.hasDnsEntries()) {
                rule.refreshDns();
            }
        }
        for (AddressRule rule : addressRules) {
            if (rule.hasDnsEntries()) {
                rule.refreshDns();
            }
        }
    }

    /**
     * Finds the access level for a user address.
     *
     * @param userAddress
     *            the IP address to check
     * @return the access level, or ACCESS_NORMAL if no rule matches
     */
    public int getUserAccessLevel(String userAddress) {
        for (AddressRule rule : userRules) {
            if (rule.matches(userAddress)) {
                return rule.getAccessLevel();
            }
        }
        return AccessManager.ACCESS_NORMAL;
    }

    /**
     * Finds the announcement message for a user address.
     *
     * @param userAddress
     *            the IP address to check
     * @return the message, or null if no rule matches or rule has no message
     */
    public String getUserAnnouncement(String userAddress) {
        for (AddressRule rule : userRules) {
            if (rule.matches(userAddress)) {
                return rule.getMessage();
            }
        }
        return null;
    }

    /**
     * Checks if an address is allowed by address rules. Returns true if no rule
     * matches (default allow).
     *
     * @param userAddress
     *            the IP address to check
     * @return true if allowed, false if denied
     */
    public boolean isAddressAllowed(String userAddress) {
        for (AddressRule rule : addressRules) {
            if (rule.matches(userAddress)) {
                return rule.isAllowed();
            }
        }
        return true;
    }

    /**
     * Checks if a game is allowed. Returns true if no rule matches (default allow).
     *
     * @param gameName
     *            the game name to check
     * @return true if allowed, false if denied
     */
    public boolean isGameAllowed(String gameName) {
        for (NameRule rule : gameRules) {
            if (rule.matches(gameName)) {
                return rule.isAllowed();
            }
        }
        return true;
    }

    /**
     * Checks if an emulator is allowed. Returns true if no rule matches (default
     * allow).
     *
     * @param emulatorName
     *            the emulator name to check
     * @return true if allowed, false if denied
     */
    public boolean isEmulatorAllowed(String emulatorName) {
        for (NameRule rule : emulatorRules) {
            if (rule.matches(emulatorName)) {
                return rule.isAllowed();
            }
        }
        return true;
    }

    /**
     * Returns the read-only list of user rules (for testing/debugging).
     */
    public List<AddressRule> getUserRules() {
        return List.copyOf(userRules);
    }

    /**
     * Returns the read-only list of address rules (for testing/debugging).
     */
    public List<AddressRule> getAddressRules() {
        return List.copyOf(addressRules);
    }

    /**
     * Returns the read-only list of game rules (for testing/debugging).
     */
    public List<NameRule> getGameRules() {
        return List.copyOf(gameRules);
    }

    /**
     * Returns the read-only list of emulator rules (for testing/debugging).
     */
    public List<NameRule> getEmulatorRules() {
        return List.copyOf(emulatorRules);
    }
}
