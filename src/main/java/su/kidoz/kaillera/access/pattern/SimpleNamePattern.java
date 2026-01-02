package su.kidoz.kaillera.access.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.emulinker.util.WildcardStringPattern;

/**
 * Simple pattern matcher for names (games, emulators). Performs
 * case-insensitive wildcard matching against a set of patterns.
 *
 * <p>
 * Pattern format: pipe-separated patterns (e.g., "Super Mario*|Zelda*")
 */
public class SimpleNamePattern implements PatternMatcher {

    private final List<WildcardStringPattern> patterns;

    /**
     * Creates a pattern matcher from a pipe-separated pattern string.
     *
     * @param patternString
     *            pipe-separated patterns (e.g., "pattern1|pattern2")
     */
    public SimpleNamePattern(String patternString) {
        List<WildcardStringPattern> tempPatterns = new ArrayList<>();

        StringTokenizer tokenizer = new StringTokenizer(patternString.toLowerCase(), "|");
        while (tokenizer.hasMoreTokens()) {
            tempPatterns.add(new WildcardStringPattern(tokenizer.nextToken()));
        }

        this.patterns = Collections.unmodifiableList(tempPatterns);
    }

    @Override
    public boolean matches(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        String lowerName = name.toLowerCase();
        for (WildcardStringPattern pattern : patterns) {
            if (pattern.match(lowerName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the number of patterns in this matcher.
     */
    public int getPatternCount() {
        return patterns.size();
    }
}
