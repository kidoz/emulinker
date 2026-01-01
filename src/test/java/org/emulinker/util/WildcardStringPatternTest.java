package org.emulinker.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for WildcardStringPattern - used by AccessManager for pattern
 * matching.
 *
 * <p>
 * Pattern types:
 * <ul>
 * <li>Exact match: "192.168.1.1"</li>
 * <li>Wildcard: "*" matches anything</li>
 * <li>Starts with: "192.168.*"</li>
 * <li>Ends with: "*.local"</li>
 * <li>Contains: "*test*"</li>
 * <li>Complex: "192.*.*1"</li>
 * </ul>
 */
@DisplayName("WildcardStringPattern Tests")
class WildcardStringPatternTest {

    @Nested
    @DisplayName("Exact Match Patterns")
    class ExactMatchPatterns {

        @Test
        @DisplayName("should match exact string")
        void shouldMatchExactString() {
            WildcardStringPattern pattern = new WildcardStringPattern("192.168.1.1");

            assertTrue(pattern.match("192.168.1.1"));
            assertFalse(pattern.match("192.168.1.2"));
            assertFalse(pattern.match("192.168.1.10"));
            assertFalse(pattern.match("10.0.0.1"));
        }

        @Test
        @DisplayName("should be case sensitive")
        void shouldBeCaseSensitive() {
            WildcardStringPattern pattern = new WildcardStringPattern("TestUser");

            assertTrue(pattern.match("TestUser"));
            assertFalse(pattern.match("testuser"));
            assertFalse(pattern.match("TESTUSER"));
        }

        @Test
        @DisplayName("should not match empty or null")
        void shouldNotMatchEmptyOrNull() {
            WildcardStringPattern pattern = new WildcardStringPattern("test");

            assertFalse(pattern.match(""));
            assertFalse(pattern.match(null));
        }
    }

    @Nested
    @DisplayName("Wildcard Only Pattern")
    class WildcardOnlyPattern {

        @Test
        @DisplayName("should match any non-empty string with * pattern")
        void shouldMatchAnyNonEmptyStringWithWildcard() {
            WildcardStringPattern pattern = new WildcardStringPattern("*");

            assertTrue(pattern.match("anything"));
            assertTrue(pattern.match("192.168.1.1"));
            assertTrue(pattern.match("a"));
            assertFalse(pattern.match(""));
            assertFalse(pattern.match(null));
        }

        @Test
        @DisplayName("should match everything with empty pattern")
        void shouldMatchEverythingWithEmptyPattern() {
            WildcardStringPattern pattern = new WildcardStringPattern("");

            assertTrue(pattern.match("anything"));
            assertTrue(pattern.match("192.168.1.1"));
            assertFalse(pattern.match(""));
            assertFalse(pattern.match(null));
        }

        @Test
        @DisplayName("should match everything with null pattern")
        void shouldMatchEverythingWithNullPattern() {
            WildcardStringPattern pattern = new WildcardStringPattern(null);

            assertTrue(pattern.match("anything"));
            assertFalse(pattern.match(""));
            assertFalse(pattern.match(null));
        }
    }

    @Nested
    @DisplayName("Starts With Patterns")
    class StartsWithPatterns {

        @Test
        @DisplayName("should match strings starting with prefix")
        void shouldMatchStringsStartingWithPrefix() {
            WildcardStringPattern pattern = new WildcardStringPattern("192.168.*");

            assertTrue(pattern.match("192.168.1.1"));
            assertTrue(pattern.match("192.168.100.200"));
            assertTrue(pattern.match("192.168."));
            assertFalse(pattern.match("192.169.1.1"));
            assertFalse(pattern.match("10.0.0.1"));
        }

        @Test
        @DisplayName("should match IP address prefixes")
        void shouldMatchIpAddressPrefixes() {
            WildcardStringPattern pattern = new WildcardStringPattern("10.0.*");

            assertTrue(pattern.match("10.0.0.1"));
            assertTrue(pattern.match("10.0.255.255"));
            assertFalse(pattern.match("10.1.0.1"));
        }
    }

    @Nested
    @DisplayName("Ends With Patterns")
    class EndsWithPatterns {

        @Test
        @DisplayName("should match strings ending with suffix")
        void shouldMatchStringsEndingWithSuffix() {
            WildcardStringPattern pattern = new WildcardStringPattern("*.local");

            assertTrue(pattern.match("server.local"));
            assertTrue(pattern.match("my-machine.local"));
            assertTrue(pattern.match(".local"));
            assertFalse(pattern.match("server.localdomain"));
            assertFalse(pattern.match("local"));
        }

        @Test
        @DisplayName("should match IP addresses ending with pattern")
        void shouldMatchIpAddressesEndingWithPattern() {
            WildcardStringPattern pattern = new WildcardStringPattern("*.1.1");

            assertTrue(pattern.match("192.168.1.1"));
            assertTrue(pattern.match("10.0.1.1"));
            assertFalse(pattern.match("192.168.1.2"));
        }
    }

    @Nested
    @DisplayName("Contains Patterns")
    class ContainsPatterns {

        @Test
        @DisplayName("should match strings containing substring")
        void shouldMatchStringsContainingSubstring() {
            WildcardStringPattern pattern = new WildcardStringPattern("*test*");

            assertTrue(pattern.match("mytest"));
            assertTrue(pattern.match("testing"));
            assertTrue(pattern.match("atestb"));
            assertTrue(pattern.match("test"));
            assertFalse(pattern.match("tset"));
            assertFalse(pattern.match("other"));
        }

        @Test
        @DisplayName("should match IP addresses containing pattern")
        void shouldMatchIpAddressesContainingPattern() {
            WildcardStringPattern pattern = new WildcardStringPattern("*168*");

            assertTrue(pattern.match("192.168.1.1"));
            assertTrue(pattern.match("168.0.0.1"));
            assertTrue(pattern.match("10.0.168.5"));
            assertFalse(pattern.match("192.167.1.1"));
        }
    }

    @Nested
    @DisplayName("Complex Patterns")
    class ComplexPatterns {

        @Test
        @DisplayName("should match with start and end wildcards")
        void shouldMatchWithStartAndEndWildcards() {
            WildcardStringPattern pattern = new WildcardStringPattern("192.*.*1");

            assertTrue(pattern.match("192.168.1.1"));
            assertTrue(pattern.match("192.0.0.1"));
            assertTrue(pattern.match("192.x.y.z1"));
            assertFalse(pattern.match("192.168.1.2"));
            assertFalse(pattern.match("10.0.0.1"));
        }

        @Test
        @DisplayName("should match emulator name patterns")
        void shouldMatchEmulatorNamePatterns() {
            WildcardStringPattern pattern = new WildcardStringPattern("mame*");

            assertTrue(pattern.match("mame"));
            assertTrue(pattern.match("mame32"));
            assertTrue(pattern.match("mame0.139"));
            assertFalse(pattern.match("MAME"));
            assertFalse(pattern.match("xmame"));
        }

        @Test
        @DisplayName("should handle multiple wildcards in middle")
        void shouldHandleMultipleWildcardsInMiddle() {
            WildcardStringPattern pattern = new WildcardStringPattern("a*b*c");

            assertTrue(pattern.match("abc"));
            assertTrue(pattern.match("aXbc"));
            assertTrue(pattern.match("abXc"));
            assertTrue(pattern.match("aXbYc"));
            assertFalse(pattern.match("ab"));
            assertFalse(pattern.match("bc"));
        }
    }

    @Nested
    @DisplayName("toString Method")
    class ToStringMethod {

        @Test
        @DisplayName("should return exact pattern for exact match")
        void shouldReturnExactPatternForExactMatch() {
            WildcardStringPattern pattern = new WildcardStringPattern("test");

            assertEquals("test", pattern.toString());
        }

        @Test
        @DisplayName("should return pattern with wildcards preserved")
        void shouldReturnPatternWithWildcardsPreserved() {
            WildcardStringPattern pattern = new WildcardStringPattern("192.*");

            assertEquals("192.*", pattern.toString());
        }

        @Test
        @DisplayName("should return complex pattern correctly")
        void shouldReturnComplexPatternCorrectly() {
            WildcardStringPattern pattern = new WildcardStringPattern("*test*");

            assertEquals("*test*", pattern.toString());
        }
    }

    @Nested
    @DisplayName("Access Control Use Cases")
    class AccessControlUseCases {

        @Test
        @DisplayName("should match local network ranges")
        void shouldMatchLocalNetworkRanges() {
            WildcardStringPattern localNet = new WildcardStringPattern("192.168.*");

            assertTrue(localNet.match("192.168.0.1"));
            assertTrue(localNet.match("192.168.100.50"));
            assertFalse(localNet.match("10.0.0.1"));
        }

        @Test
        @DisplayName("should match specific user patterns")
        void shouldMatchSpecificUserPatterns() {
            WildcardStringPattern adminPattern = new WildcardStringPattern("admin*");

            assertTrue(adminPattern.match("admin"));
            assertTrue(adminPattern.match("administrator"));
            assertTrue(adminPattern.match("admin123"));
            assertFalse(adminPattern.match("sysadmin"));
        }

        @Test
        @DisplayName("should match banned game ROM patterns")
        void shouldMatchBannedGameRomPatterns() {
            WildcardStringPattern bannedRom = new WildcardStringPattern("*hack*");

            assertTrue(bannedRom.match("super_hack_v1"));
            assertTrue(bannedRom.match("hacked_game"));
            assertTrue(bannedRom.match("myhackrom"));
            assertFalse(bannedRom.match("legitimate_game"));
        }
    }
}
