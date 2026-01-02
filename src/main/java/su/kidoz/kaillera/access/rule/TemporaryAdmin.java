package su.kidoz.kaillera.access.rule;

import su.kidoz.kaillera.access.TimedAccessRule;

/**
 * Temporary admin grant that expires after a specified duration. When matched,
 * grants admin access level.
 */
public class TemporaryAdmin extends TimedAccessRule {

    /**
     * Creates a temporary admin rule.
     *
     * @param addressPattern
     *            pipe-separated IP address patterns
     * @param minutes
     *            duration in minutes before the admin grant expires
     */
    public TemporaryAdmin(String addressPattern, int minutes) {
        super(addressPattern, minutes);
    }
}
