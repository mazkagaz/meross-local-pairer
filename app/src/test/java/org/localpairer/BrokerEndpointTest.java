package org.localpairer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BrokerEndpointTest {
    @Test
    public void derivedUrlsUseDefaultPorts() {
        assertEquals("https://homeassistant.local:2004",
                new BrokerEndpoint("homeassistant.local", true,
                        BrokerEndpoint.DEFAULT_HTTP_PORT,
                        BrokerEndpoint.DEFAULT_HTTPS_PORT).baseUrl());
        assertEquals("http://192.168.1.10:2003",
                new BrokerEndpoint("192.168.1.10", false,
                        BrokerEndpoint.DEFAULT_HTTP_PORT,
                        BrokerEndpoint.DEFAULT_HTTPS_PORT).baseUrl());
    }

    @Test
    public void customPortsAreUsedPerScheme() {
        BrokerEndpoint endpoint = new BrokerEndpoint("homeassistant.local", true, 12003, 12004);
        assertEquals(12004, endpoint.selectedPort());
        assertEquals("https://homeassistant.local:12004", endpoint.baseUrl());
        assertEquals("http://homeassistant.local:12003", endpoint.withHttps(false).baseUrl());
    }

    @Test
    public void hostSanitizationStripsSchemePathAndPort() {
        assertEquals("homeassistant.local",
                BrokerEndpoint.sanitizeHost("http://homeassistant.local:2003/path?q=1"));
        assertEquals("192.168.1.10",
                BrokerEndpoint.sanitizeHost("https://user@192.168.1.10:2004"));
        assertEquals("haproxy", BrokerEndpoint.sanitizeHost("  haproxy  "));
        assertEquals("[fe80::1]", BrokerEndpoint.sanitizeHost("[fe80::1]:2004"));
    }

    @Test
    public void migrationParsesStoredUrl() {
        BrokerEndpoint endpoint = BrokerEndpoint.fromUrl("https://homeassistant.local:8443");
        assertEquals("homeassistant.local", endpoint.getHost());
        assertTrue(endpoint.isHttps());
        assertEquals(8443, endpoint.getHttpsPort());
        assertEquals(BrokerEndpoint.DEFAULT_HTTP_PORT, endpoint.getHttpPort());

        BrokerEndpoint http = BrokerEndpoint.fromUrl("http://192.168.1.10:2003");
        assertFalse(http.isHttps());
        assertEquals(2003, http.getHttpPort());
    }

    @Test
    public void invalidPortsFallBackToDefaults() {
        BrokerEndpoint endpoint = new BrokerEndpoint("host", false, 0, 70000);
        assertEquals(BrokerEndpoint.DEFAULT_HTTP_PORT, endpoint.getHttpPort());
        assertEquals(BrokerEndpoint.DEFAULT_HTTPS_PORT, endpoint.getHttpsPort());
    }

    @Test
    public void emptyInputIsInvalid() {
        assertNull(BrokerEndpoint.fromUrl("   "));
        assertFalse(new BrokerEndpoint("", false,
                BrokerEndpoint.DEFAULT_HTTP_PORT,
                BrokerEndpoint.DEFAULT_HTTPS_PORT).isValid());
    }

    @Test
    public void cleartextIpRequiresHttps() {
        BrokerEndpoint ip = new BrokerEndpoint("192.168.1.10", false,
                BrokerEndpoint.DEFAULT_HTTP_PORT, BrokerEndpoint.DEFAULT_HTTPS_PORT);
        assertTrue(ip.requiresHttps());
        assertFalse(ip.withHttps(true).requiresHttps());

        BrokerEndpoint ipv6 = new BrokerEndpoint("[fe80::1]", false,
                BrokerEndpoint.DEFAULT_HTTP_PORT, BrokerEndpoint.DEFAULT_HTTPS_PORT);
        assertTrue(ipv6.requiresHttps());

        BrokerEndpoint hostname = new BrokerEndpoint("homeassistant.local", false,
                BrokerEndpoint.DEFAULT_HTTP_PORT, BrokerEndpoint.DEFAULT_HTTPS_PORT);
        assertFalse(hostname.requiresHttps());
    }
}
