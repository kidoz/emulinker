package su.kidoz.kaillera.access.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.emulinker.kaillera.access.AccessException;
import org.emulinker.kaillera.access.AccessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import su.kidoz.kaillera.access.rule.AddressRule;
import su.kidoz.kaillera.access.rule.NameRule;

/**
 * Parser for access configuration files. Extracts rules from the config file
 * and returns them as a structured ParseResult.
 *
 * <p>
 * Config file format:
 * <ul>
 * <li>{@code user, <access>, <patterns> [, <message>]} - user access levels
 * <li>{@code ipaddress, <allow|deny>, <patterns>} - IP allow/deny rules
 * <li>{@code game, <allow|deny>, <patterns>} - game allow/deny rules
 * <li>{@code emulator, <allow|deny>, <patterns>} - emulator allow/deny rules
 * </ul>
 * Lines starting with # or // are comments.
 */
public class AccessConfigParser {

    private static final Logger log = LoggerFactory.getLogger(AccessConfigParser.class);

    /**
     * Parses an access configuration file.
     *
     * @param accessFile
     *            the file to parse
     * @return the parsed rules
     * @throws IOException
     *             if the file cannot be read
     */
    public ParseResult parse(File accessFile) throws IOException {
        List<AddressRule> userRules = new ArrayList<>();
        List<AddressRule> addressRules = new ArrayList<>();
        List<NameRule> gameRules = new ArrayList<>();
        List<NameRule> emulatorRules = new ArrayList<>();
        int errorCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(accessFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isCommentOrEmpty(line)) {
                    continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length < 3) {
                    log.error("Failed to load access line, too few tokens: {}", line);
                    errorCount++;
                    continue;
                }

                String type = tokens[0].trim().toLowerCase();
                String remainder = line.substring(type.length() + 1);
                StringTokenizer tokenizer = new StringTokenizer(remainder, ",");

                try {
                    switch (type) {
                        case "user" :
                            userRules.add(parseUserRule(tokenizer));
                            break;
                        case "ipaddress" :
                            addressRules.add(parseAddressRule(tokenizer));
                            break;
                        case "game" :
                            gameRules.add(parseGameRule(tokenizer));
                            break;
                        case "emulator" :
                            emulatorRules.add(parseEmulatorRule(tokenizer));
                            break;
                        default :
                            log.error("Unrecognized access type: {} in line: {}", type, line);
                            errorCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to load access line: {}: {}", e.getMessage(), line);
                    errorCount++;
                }
            }
        }

        return new ParseResult(Collections.unmodifiableList(userRules),
                Collections.unmodifiableList(addressRules), Collections.unmodifiableList(gameRules),
                Collections.unmodifiableList(emulatorRules), errorCount);
    }

    private boolean isCommentOrEmpty(String line) {
        return line.isEmpty() || line.startsWith("#") || line.startsWith("//");
    }

    private AddressRule parseUserRule(StringTokenizer tokenizer) throws AccessException {
        int tokenCount = tokenizer.countTokens();
        if (tokenCount < 2 || tokenCount > 3) {
            throw new AccessException("Wrong number of tokens: " + tokenCount);
        }

        String accessStr = tokenizer.nextToken().trim().toLowerCase();
        int accessLevel = parseAccessLevel(accessStr);
        String patterns = tokenizer.nextToken().trim();
        String message = tokenizer.hasMoreTokens() ? tokenizer.nextToken().trim() : null;

        return AddressRule.forUser(patterns, accessLevel, message);
    }

    private int parseAccessLevel(String accessStr) throws AccessException {
        return switch (accessStr) {
            case "normal" -> AccessManager.ACCESS_NORMAL;
            case "elevated" -> AccessManager.ACCESS_ELEVATED;
            case "admin" -> AccessManager.ACCESS_ADMIN;
            default -> throw new AccessException("Unrecognized access token: " + accessStr);
        };
    }

    private AddressRule parseAddressRule(StringTokenizer tokenizer) throws AccessException {
        if (tokenizer.countTokens() != 2) {
            throw new AccessException("Wrong number of tokens: " + tokenizer.countTokens());
        }

        String accessStr = tokenizer.nextToken().trim().toLowerCase();
        boolean allowed = parseAllowDeny(accessStr);
        String patterns = tokenizer.nextToken().trim();

        return AddressRule.forAddress(patterns, allowed);
    }

    private NameRule parseGameRule(StringTokenizer tokenizer) throws AccessException {
        if (tokenizer.countTokens() != 2) {
            throw new AccessException("Wrong number of tokens: " + tokenizer.countTokens());
        }

        String accessStr = tokenizer.nextToken().trim().toLowerCase();
        boolean allowed = parseAllowDeny(accessStr);
        String patterns = tokenizer.nextToken().trim();

        return NameRule.forGame(patterns, allowed);
    }

    private NameRule parseEmulatorRule(StringTokenizer tokenizer) throws AccessException {
        if (tokenizer.countTokens() != 2) {
            throw new AccessException("Wrong number of tokens: " + tokenizer.countTokens());
        }

        String accessStr = tokenizer.nextToken().trim().toLowerCase();
        boolean allowed = parseAllowDeny(accessStr);
        String patterns = tokenizer.nextToken().trim();

        return NameRule.forEmulator(patterns, allowed);
    }

    private boolean parseAllowDeny(String accessStr) throws AccessException {
        return switch (accessStr) {
            case "allow" -> true;
            case "deny" -> false;
            default -> throw new AccessException("Unrecognized access token: " + accessStr);
        };
    }
}
