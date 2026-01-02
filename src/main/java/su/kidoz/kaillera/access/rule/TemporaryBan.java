package su.kidoz.kaillera.access.rule;

import su.kidoz.kaillera.access.TimedAccessRule;

/**
 * Temporary ban rule that expires after a specified duration. When matched,
 * denies access to the server.
 */
public class TemporaryBan extends TimedAccessRule {

    /**
     * Creates a temporary ban rule.
     *
     * @param addressPattern
     *            pipe-separated IP address patterns
     * @param minutes
     *            duration in minutes before the ban expires
     */
    public TemporaryBan(String addressPattern, int minutes) {
        super(addressPattern, minutes);
    }
}
