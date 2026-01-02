package su.kidoz.kaillera.access.pattern;

/**
 * Interface for pattern matching in access control rules. Implementations can
 * match IP addresses, hostnames, or names.
 */
public interface PatternMatcher {

    /**
     * Checks if the given input matches this pattern.
     *
     * @param input
     *            the string to match against (e.g., IP address, game name)
     * @return true if the input matches this pattern
     */
    boolean matches(String input);
}
