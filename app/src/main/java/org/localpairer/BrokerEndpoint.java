package org.localpairer;

import java.net.URI;

/**
 * Broker HTTP API endpoint derived from the Home Assistant host and the
 * HTTP/HTTPS choice. Users never type a scheme or a port in the default UI.
 */
public final class BrokerEndpoint {
    public static final int DEFAULT_HTTP_PORT = 2003;
    public static final int DEFAULT_HTTPS_PORT = 2004;

    private final String host;
    private final boolean https;
    private final int httpPort;
    private final int httpsPort;

    public BrokerEndpoint(String host, boolean https, int httpPort, int httpsPort) {
        this.host = sanitizeHost(host);
        this.https = https;
        this.httpPort = validPort(httpPort, DEFAULT_HTTP_PORT);
        this.httpsPort = validPort(httpsPort, DEFAULT_HTTPS_PORT);
    }

    public String getHost() {
        return host;
    }

    public boolean isHttps() {
        return https;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public int selectedPort() {
        return https ? httpsPort : httpPort;
    }

    public String baseUrl() {
        return (https ? "https://" : "http://") + host + ":" + selectedPort();
    }

    public boolean isValid() {
        return host != null && !host.isEmpty();
    }

    /** Cleartext is not permitted by the network security config for an IP endpoint. */
    public boolean requiresHttps() {
        return !https && isIpAddress(host);
    }

    public static boolean isIpAddress(String host) {
        if (host == null) {
            return false;
        }
        String value = host.trim();
        if (value.startsWith("[") && value.endsWith("]") && value.length() > 2) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.indexOf(':') >= 0) {
            return true;
        }
        String[] parts = value.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || !isNumeric(part)) {
                return false;
            }
            if (Integer.parseInt(part) > 255) {
                return false;
            }
        }
        return true;
    }

    public BrokerEndpoint withHttps(boolean useHttps) {
        return new BrokerEndpoint(host, useHttps, httpPort, httpsPort);
    }

    /**
     * Accepts a host, IP address or a pasted URL and keeps only the host part:
     * strips scheme, credentials, path, query, fragment and port. IPv6
     * literals keep their brackets.
     */
    public static String sanitizeHost(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        int scheme = value.indexOf("://");
        if (scheme >= 0) {
            value = value.substring(scheme + 3);
        }
        int cut = firstIndexOf(value, '/', '?', '#');
        if (cut >= 0) {
            value = value.substring(0, cut);
        }
        int at = value.lastIndexOf('@');
        if (at >= 0) {
            value = value.substring(at + 1);
        }
        if (value.startsWith("[")) {
            int close = value.indexOf(']');
            if (close > 0) {
                return value.substring(0, close + 1);
            }
            return "";
        }
        int colon = value.lastIndexOf(':');
        if (colon >= 0 && value.indexOf(':') == colon) {
            String port = value.substring(colon + 1);
            if (!port.isEmpty() && isNumeric(port)) {
                value = value.substring(0, colon);
            }
        }
        return value.trim();
    }

    /**
     * Parse a stored API URL for migration. Returns null when no host can be
     * extracted.
     */
    public static BrokerEndpoint fromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String value = url.trim();
        boolean https = value.regionMatches(true, 0, "https://", 0, 8);
        int httpPort = DEFAULT_HTTP_PORT;
        int httpsPort = DEFAULT_HTTPS_PORT;
        try {
            String normalized = value.contains("://") ? value : "http://" + value;
            URI uri = new URI(normalized);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                host = sanitizeHost(value);
            }
            int port = uri.getPort();
            if (port > 0) {
                if (https) {
                    httpsPort = port;
                } else {
                    httpPort = port;
                }
            }
            return new BrokerEndpoint(host, https, httpPort, httpsPort);
        } catch (Exception e) {
            String host = sanitizeHost(value);
            return host.isEmpty() ? null : new BrokerEndpoint(host, https, httpPort, httpsPort);
        }
    }

    private static int validPort(int value, int fallback) {
        return value >= 1 && value <= 65535 ? value : fallback;
    }

    private static int firstIndexOf(String value, char... candidates) {
        int best = -1;
        for (char candidate : candidates) {
            int index = value.indexOf(candidate);
            if (index >= 0 && (best < 0 || index < best)) {
                best = index;
            }
        }
        return best;
    }

    private static boolean isNumeric(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
