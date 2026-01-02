package su.kidoz.kaillera.access.store;

import java.util.concurrent.CopyOnWriteArrayList;

import su.kidoz.kaillera.access.rule.TemporaryAdmin;
import su.kidoz.kaillera.access.rule.TemporaryBan;
import su.kidoz.kaillera.access.rule.TemporarySilence;

/**
 * Thread-safe store for temporary access rules (bans, admin grants, silences).
 * Uses CopyOnWriteArrayList for safe concurrent access.
 */
public class TemporaryRuleStore {

    private final CopyOnWriteArrayList<TemporaryBan> bans = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<TemporaryAdmin> admins = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<TemporarySilence> silences = new CopyOnWriteArrayList<>();

    /**
     * Adds a temporary ban.
     *
     * @param addressPattern
     *            pipe-separated IP address patterns
     * @param minutes
     *            duration in minutes
     */
    public void addBan(String addressPattern, int minutes) {
        bans.add(new TemporaryBan(addressPattern, minutes));
    }

    /**
     * Adds a temporary admin grant.
     *
     * @param addressPattern
     *            pipe-separated IP address patterns
     * @param minutes
     *            duration in minutes
     */
    public void addAdmin(String addressPattern, int minutes) {
        admins.add(new TemporaryAdmin(addressPattern, minutes));
    }

    /**
     * Adds a temporary silence.
     *
     * @param addressPattern
     *            pipe-separated IP address patterns
     * @param minutes
     *            duration in minutes
     */
    public void addSilence(String addressPattern, int minutes) {
        silences.add(new TemporarySilence(addressPattern, minutes));
    }

    /**
     * Removes all expired temporary rules.
     */
    public void purgeExpired() {
        bans.removeIf(TemporaryBan::isExpired);
        admins.removeIf(TemporaryAdmin::isExpired);
        silences.removeIf(TemporarySilence::isExpired);
    }

    /**
     * Clears all temporary rules for a specific address.
     *
     * @param userAddress
     *            the IP address to clear rules for
     * @return true if any rules were removed
     */
    public boolean clearForAddress(String userAddress) {
        boolean foundSilence = silences.removeIf(s -> s.matches(userAddress));
        boolean foundBan = bans.removeIf(b -> b.matches(userAddress) && !b.isExpired());
        boolean foundAdmin = admins.removeIf(a -> a.matches(userAddress));
        return foundSilence || foundBan || foundAdmin;
    }

    /**
     * Clears all temporary rules.
     */
    public void clear() {
        bans.clear();
        admins.clear();
        silences.clear();
    }

    /**
     * Checks if an address is temporarily banned.
     *
     * @param userAddress
     *            the IP address to check
     * @return true if the address is banned and ban has not expired
     */
    public boolean isBanned(String userAddress) {
        for (TemporaryBan ban : bans) {
            if (ban.matches(userAddress) && !ban.isExpired()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an address has temporary admin.
     *
     * @param userAddress
     *            the IP address to check
     * @return true if the address has admin and grant has not expired
     */
    public boolean hasTemporaryAdmin(String userAddress) {
        for (TemporaryAdmin admin : admins) {
            if (admin.matches(userAddress) && !admin.isExpired()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an address is temporarily silenced.
     *
     * @param userAddress
     *            the IP address to check
     * @return true if the address is silenced and silence has not expired
     */
    public boolean isSilenced(String userAddress) {
        for (TemporarySilence silence : silences) {
            if (silence.matches(userAddress) && !silence.isExpired()) {
                return true;
            }
        }
        return false;
    }
}
