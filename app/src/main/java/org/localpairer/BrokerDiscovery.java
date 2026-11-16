package org.localpairer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Parsing helpers for the broker's mDNS TXT records. */
public final class BrokerDiscovery {
    private BrokerDiscovery() {
    }

    public static String decode(Map<String, byte[]> attributes, String key) {
        if (attributes == null || key == null) {
            return null;
        }
        byte[] value = attributes.get(key);
        if (value == null || value.length == 0) {
            return null;
        }
        return new String(value, StandardCharsets.UTF_8).trim();
    }

    /**
     * Builds the ordered host candidates: the TXT list first, then the
     * mDNS-resolved address as a fallback.
     */
    public static List<String> hostCandidates(String hostTxt, String fallback) {
        List<String> candidates = new ArrayList<>();
        if (hostTxt != null) {
            for (String part : hostTxt.split(",")) {
                String host = BrokerEndpoint.sanitizeHost(part);
                if (!host.isEmpty() && !candidates.contains(host)) {
                    candidates.add(host);
                }
            }
        }
        String resolved = BrokerEndpoint.sanitizeHost(fallback);
        if (!resolved.isEmpty() && !candidates.contains(resolved)) {
            candidates.add(resolved);
        }
        return candidates;
    }

    public static int parsePort(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            int port = Integer.parseInt(value.trim());
            return port >= 0 && port <= 65535 ? port : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
