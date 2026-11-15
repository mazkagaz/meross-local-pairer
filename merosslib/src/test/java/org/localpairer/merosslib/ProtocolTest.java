package org.localpairer.merosslib;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;
import org.localpairer.merosslib.model.protocol.MessageSetConfigKey;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProtocolTest {

    @Test
    public void configKeyUsesNumericPortsAndRedirect() {
        MessageSetConfigKey message = MessageSetConfigKey.BuildNew(
                "broker.local", 2001, "secret-key", "12345");
        JsonObject json = JsonParser.parseString(
                Utils.getGson().toJson(message)).getAsJsonObject();

        JsonObject header = json.getAsJsonObject("header");
        assertEquals("Appliance.Config.Key", header.get("namespace").getAsString());
        assertEquals("SET", header.get("method").getAsString());

        JsonObject gateway = json.getAsJsonObject("payload")
                .getAsJsonObject("key").getAsJsonObject("gateway");
        assertTrue(gateway.get("port").getAsJsonPrimitive().isNumber());
        assertEquals(2001, gateway.get("port").getAsInt());
        assertTrue(gateway.get("secondPort").getAsJsonPrimitive().isNumber());
        assertEquals(2001, gateway.get("secondPort").getAsInt());
        assertEquals(0, gateway.get("redirect").getAsInt());
    }

    @Test
    public void acceptsSetAck() throws Exception {
        HttpServer server = startServer("{" +
                "\"header\":{\"messageId\":\"1\",\"namespace\":\"Appliance.Config.Key\"," +
                "\"method\":\"SETACK\",\"timestamp\":0,\"sign\":\"x\"},\"payload\":{}}");
        try {
            MerossDeviceAp device = new MerossDeviceAp("127.0.0.1:" + server.getAddress().getPort(), "");
            assertNotNull(device.setConfigKey("broker.local", 2001, "key", "1"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void rejectsErrorResponse() throws Exception {
        HttpServer server = startServer("{" +
                "\"header\":{\"messageId\":\"1\",\"namespace\":\"Appliance.Config.Key\"," +
                "\"method\":\"ERROR\",\"timestamp\":0,\"sign\":\"x\"}," +
                "\"payload\":{\"error\":{\"code\":\"1000\"}}}");
        try {
            MerossDeviceAp device = new MerossDeviceAp("127.0.0.1:" + server.getAddress().getPort(), "");
            device.setConfigKey("broker.local", 2001, "key", "1");
            fail("Expected IOException for ERROR response");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("refused"));
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startServer(String responseBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/config", exchange -> {
            exchange.getRequestBody().close();
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return server;
    }
}
