package org.localpairer;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BrokerDiscoveryTest {
    @Test
    public void decodesTxtValues() {
        Map<String, byte[]> attributes = new HashMap<>();
        attributes.put("host", "192.168.1.10".getBytes(StandardCharsets.UTF_8));
        attributes.put("https_port", "2004".getBytes(StandardCharsets.UTF_8));
        assertEquals("192.168.1.10", BrokerDiscovery.decode(attributes, "host"));
        assertEquals("2004", BrokerDiscovery.decode(attributes, "https_port"));
        assertNull(BrokerDiscovery.decode(attributes, "missing"));
    }

    @Test
    public void hostCandidatesPreferTxtAndKeepFallbackLast() {
        assertEquals(Arrays.asList("192.168.1.10", "192.168.1.11", "homeassistant.local"),
                BrokerDiscovery.hostCandidates("192.168.1.10, 192.168.1.11",
                        "homeassistant.local"));
        assertEquals(Arrays.asList("homeassistant.local"),
                BrokerDiscovery.hostCandidates(null, "homeassistant.local"));
    }

    @Test
    public void parsesPortsWithZeroAllowed() {
        assertEquals(2004, BrokerDiscovery.parsePort("2004", 0));
        assertEquals(0, BrokerDiscovery.parsePort("0", 2004));
        assertEquals(2003, BrokerDiscovery.parsePort("nope", 2003));
        assertEquals(2003, BrokerDiscovery.parsePort("70000", 2003));
    }
}
