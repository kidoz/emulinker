package su.kidoz.kaillera.access.rule;

import su.kidoz.kaillera.access.pattern.SimpleNamePattern;

/**
 * Unified rule for name-based access control. Used for both game and emulator
 * allow/deny rules.
 *
 * <p>
 * This class unifies the previous GameAccess and EmulatorAccess inner classes,
 * which were nearly identical.
 */
public final class NameRule {

    /**
     * Type of name rule.
     */
    public enum Type {
        /** Game name rule */
        GAME,
        /** Emulator name rule */
        EMULATOR
    }

    private final Type type;
    private final SimpleNamePattern pattern;
    private final boolean allowed;

    private NameRule(Type type, SimpleNamePattern pattern, boolean allowed) {
        this.type = type;
        this.pattern = pattern;
        this.allowed = allowed;
    }

    /**
     * Creates a game access rule.
     *
     * @param patternString
     *            pipe-separated game name patterns
     * @param allowed
     *            true to allow, false to deny
     * @return the game rule
     */
    public static NameRule forGame(String patternString, boolean allowed) {
        return new NameRule(Type.GAME, new SimpleNamePattern(patternString), allowed);
    }

    /**
     * Creates an emulator access rule.
     *
     * @param patternString
     *            pipe-separated emulator name patterns
     * @param allowed
     *            true to allow, false to deny
     * @return the emulator rule
     */
    public static NameRule forEmulator(String patternString, boolean allowed) {
        return new NameRule(Type.EMULATOR, new SimpleNamePattern(patternString), allowed);
    }

    /**
     * Returns the rule type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Checks if the given name matches this rule's pattern.
     *
     * @param name
     *            the game or emulator name to check
     * @return true if the name matches
     */
    public boolean matches(String name) {
        return pattern.matches(name);
    }

    /**
     * Returns whether access is allowed.
     */
    public boolean isAllowed() {
        return allowed;
    }
}
