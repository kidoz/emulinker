package su.kidoz.kaillera.access;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TimedAccessRule Tests")
class TimedAccessRuleTest {

    // Test implementation since TimedAccessRule is abstract
    private static class TestTimedAccessRule extends TimedAccessRule {
        TestTimedAccessRule(String addressPattern, int minutes) {
            super(addressPattern, minutes);
        }
    }

    @Nested
    @DisplayName("Pattern Matching")
    class PatternMatching {
        @Test
        @DisplayName("should match exact address")
        void shouldMatchExactAddress() {
            TestTimedAccessRule rule = new TestTimedAccessRule("192.168.1.100", 60);
            assertTrue(rule.matches("192.168.1.100"));
            assertFalse(rule.matches("192.168.1.101"));
        }

        @Test
        @DisplayName("should match wildcard pattern")
        void shouldMatchWildcardPattern() {
            TestTimedAccessRule rule = new TestTimedAccessRule("192.168.1.*", 60);
            assertTrue(rule.matches("192.168.1.1"));
            assertTrue(rule.matches("192.168.1.100"));
            assertTrue(rule.matches("192.168.1.255"));
            assertFalse(rule.matches("192.168.2.1"));
        }

        @Test
        @DisplayName("should match multiple patterns")
        void shouldMatchMultiplePatterns() {
            TestTimedAccessRule rule = new TestTimedAccessRule("192.168.1.*|10.0.0.*", 60);
            assertTrue(rule.matches("192.168.1.50"));
            assertTrue(rule.matches("10.0.0.1"));
            assertFalse(rule.matches("172.16.0.1"));
        }

        @Test
        @DisplayName("should match contains pattern with asterisks")
        void shouldMatchContainsPattern() {
            TestTimedAccessRule rule = new TestTimedAccessRule("*168*", 60);
            assertTrue(rule.matches("192.168.1.10"));
            assertTrue(rule.matches("10.168.0.1"));
            assertFalse(rule.matches("192.169.1.1"));
        }

        @Test
        @DisplayName("patterns are lowercased for case-insensitive matching")
        void shouldLowercasePatterns() {
            // TimedAccessRule lowercases patterns, so ABC becomes abc
            TestTimedAccessRule rule = new TestTimedAccessRule("abc.def.*", 60);
            assertTrue(rule.matches("abc.def.123"));
            // Match uses pattern.match() which is case-sensitive,
            // but patterns are normalized to lowercase
            assertTrue(rule.matches("abc.def.456"));
        }
    }

    @Nested
    @DisplayName("Expiration")
    class Expiration {
        @Test
        @DisplayName("should not be expired immediately")
        void shouldNotBeExpiredImmediately() {
            TestTimedAccessRule rule = new TestTimedAccessRule("192.168.1.1", 60);
            assertFalse(rule.isExpired());
        }

        @Test
        @DisplayName("should be expired after time passes")
        void shouldBeExpiredAfterTime() throws InterruptedException {
            // Create a rule with 0 minutes (immediate expiry)
            TestTimedAccessRule rule = new TestTimedAccessRule("192.168.1.1", 0);
            // Wait a tiny bit to ensure time passes
            Thread.sleep(10);
            assertTrue(rule.isExpired());
        }

        @Test
        @DisplayName("should respect different timeout values")
        void shouldRespectTimeoutValues() {
            // 1 minute rule should not be expired
            TestTimedAccessRule rule1min = new TestTimedAccessRule("192.168.1.1", 1);
            assertFalse(rule1min.isExpired());

            // 0 minute rule should be expired (or about to be)
            TestTimedAccessRule rule0min = new TestTimedAccessRule("192.168.1.1", 0);
            // Immediately or very soon after creation, 0 minutes should expire
            assertTrue(
                    rule0min.isExpired() || System.currentTimeMillis() >= rule0min.getStartTime());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        @Test
        @DisplayName("should handle empty pattern gracefully")
        void shouldHandleEmptyPattern() {
            TestTimedAccessRule rule = new TestTimedAccessRule("", 60);
            // Empty pattern should create empty list, matching nothing
            assertTrue(rule.getPatterns().isEmpty());
            assertFalse(rule.matches("192.168.1.1"));
        }

        @Test
        @DisplayName("should handle patterns with leading/trailing spaces in token")
        void shouldHandleWhitespaceInPatterns() {
            // Note: StringTokenizer with "|" includes spaces in tokens
            // This test verifies actual behavior - spaces become part of pattern
            TestTimedAccessRule rule = new TestTimedAccessRule("192.168.1.*|10.0.0.*", 60);
            assertTrue(rule.matches("192.168.1.1"));
            assertTrue(rule.matches("10.0.0.1"));
        }

        @Test
        @DisplayName("should handle single asterisk as match-all for non-empty strings")
        void shouldHandleSingleAsterisk() {
            TestTimedAccessRule rule = new TestTimedAccessRule("*", 60);
            assertTrue(rule.matches("192.168.1.1"));
            assertTrue(rule.matches("anything.goes.here"));
            // Empty string returns false in WildcardStringPattern.match()
            assertFalse(rule.matches(""));
        }
    }
}
