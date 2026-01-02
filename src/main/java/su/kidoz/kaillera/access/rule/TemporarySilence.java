package su.kidoz.kaillera.access.rule;

import su.kidoz.kaillera.access.TimedAccessRule;

/**
 * Temporary silence rule that expires after a specified duration. When matched,
 * prevents the user from chatting.
 */
public class TemporarySilence extends TimedAccessRule {

    /**
     * Creates a temporary silence rule.
     *
     * @param addressPattern
     *            pipe-separated IP address patterns
     * @param minutes
     *            duration in minutes before the silence expires
     */
    public TemporarySilence(String addressPattern, int minutes) {
        super(addressPattern, minutes);
    }
}
