package su.kidoz.kaillera.access.pattern;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import su.kidoz.util.WildcardStringPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pattern matcher that supports both wildcard IP patterns and DNS hostname
 * resolution. Hostnames are periodically resolved to IP addresses to support
 * dynamic DNS.
 *
 * <p>
 * Pattern format: pipe-separated patterns (e.g., "192.168.*|dns:example.com")
 * <ul>
 * <li>Regular patterns: matched using wildcard pattern matching
 * <li>DNS patterns: prefixed with "dns:", resolved to IP addresses
 * </ul>
 */
public class DnsResolvingPattern implements PatternMatcher {

    private static final Logger log = LoggerFactory.getLogger(DnsResolvingPattern.class);
    private static final String DNS_PREFIX = "dns:";

    private final List<WildcardStringPattern> patterns;
    private final List<String> hostNames;
    private volatile List<String> resolvedAddresses;

    /**
     * Creates a pattern matcher from a pipe-separated pattern string.
     *
     * @param patternString
     *            pipe-separated patterns (e.g., "192.168.*|dns:example.com")
     */
    public DnsResolvingPattern(String patternString) {
        List<WildcardStringPattern> tempPatterns = new ArrayList<>();
        List<String> tempHostNames = new ArrayList<>();

        StringTokenizer tokenizer = new StringTokenizer(patternString.toLowerCase(), "|");
        while (tokenizer.hasMoreTokens()) {
            String pattern = tokenizer.nextToken();
            if (pattern.startsWith(DNS_PREFIX)) {
                String hostName = pattern.substring(DNS_PREFIX.length());
                if (!hostName.isEmpty()) {
                    tempHostNames.add(hostName);
                    logInitialDnsResolution(hostName);
                } else {
                    log.warn("Empty DNS hostname in pattern: {}", patternString);
                }
            } else {
                tempPatterns.add(new WildcardStringPattern(pattern));
            }
        }

        this.patterns = Collections.unmodifiableList(tempPatterns);
        this.hostNames = Collections.unmodifiableList(tempHostNames);
        this.resolvedAddresses = new ArrayList<>();

        refreshDns();
    }

    private void logInitialDnsResolution(String hostName) {
        try {
            InetAddress address = InetAddress.getByName(hostName);
            log.debug("Resolved {} to {}", hostName, address.getHostAddress());
        } catch (UnknownHostException e) {
            log.warn("Failed to resolve DNS entry to an address: {}: {}", hostName, e.getMessage());
        }
    }

    /**
     * Refreshes all DNS hostname resolutions. Should be called periodically to
     * handle dynamic DNS changes.
     */
    public void refreshDns() {
        List<String> newAddresses = new ArrayList<>();
        for (String hostName : hostNames) {
            try {
                InetAddress address = InetAddress.getByName(hostName);
                newAddresses.add(address.getHostAddress());
            } catch (UnknownHostException e) {
                log.debug("Failed to resolve DNS entry to an address: {}", hostName, e);
            }
        }
        // Atomic replacement for thread safety
        this.resolvedAddresses = newAddresses;
    }

    @Override
    public boolean matches(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        // Check wildcard patterns
        for (WildcardStringPattern pattern : patterns) {
            if (pattern.match(address)) {
                return true;
            }
        }

        // Check resolved DNS addresses
        List<String> currentAddresses = resolvedAddresses;
        for (String resolved : currentAddresses) {
            if (resolved.equals(address)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the list of hostnames configured for DNS resolution.
     */
    public List<String> getHostNames() {
        return hostNames;
    }

    /**
     * Returns the currently resolved addresses.
     */
    public List<String> getResolvedAddresses() {
        return new ArrayList<>(resolvedAddresses);
    }

    /**
     * Returns whether this pattern has any DNS entries.
     */
    public boolean hasDnsEntries() {
        return !hostNames.isEmpty();
    }
}
