package org.localpairer.ble;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Meross BLE JSON framing and payload helpers. */
public final class MerossBleProtocol {
    private MerossBleProtocol() {
    }

    public static JSONObject packet(String namespace, String method, JSONObject payload) {
        JSONObject header = new JSONObject();
        try {
            String messageId = md5Hex(UUID.randomUUID().toString());
            int timestamp = (int) (System.currentTimeMillis() / 1000);
            header.put("from", "");
            header.put("messageId", messageId);
            header.put("method", method);
            header.put("namespace", namespace);
            header.put("payloadVersion", 1);
            header.put("sign", md5Hex(messageId + "key" + timestamp));
            header.put("timestamp", timestamp);
            header.put("triggerSrc", "AndroidLocal");

            JSONObject packet = new JSONObject();
            packet.put("header", header);
            packet.put("payload", payload != null ? payload : new JSONObject());
            return packet;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String md5Hex(String value) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String encryptWifiPassword(String password, String type,
                                             String uuid, String mac) {
        try {
            String keyHex = md5Hex(type + uuid + mac);
            int paddedLength = ((password.length() / 16) + 1) * 16;
            StringBuilder padded = new StringBuilder(password);
            while (padded.length() < paddedLength) {
                padded.append('\0');
            }
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec key = new SecretKeySpec(
                    keyHex.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec iv = new IvParameterSpec(
                    "0000000000000000".getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] encrypted = cipher.doFinal(
                    padded.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static final class WifiEntry {
        public final String ssid;
        public final String bssid;
        public final int channel;
        public final int signal;
        public final int encryption;
        public final int cipher;

        WifiEntry(String ssid, String bssid, int channel, int signal,
                  int encryption, int cipher) {
            this.ssid = ssid;
            this.bssid = bssid;
            this.channel = channel;
            this.signal = signal;
            this.encryption = encryption;
            this.cipher = cipher;
        }

        public JSONObject toPayload(String encryptedPassword) throws Exception {
            JSONObject wifi = new JSONObject();
            wifi.put("cipher", cipher);
            wifi.put("password", encryptedPassword);
            wifi.put("encryption", encryption);
            wifi.put("bssid", bssid);
            wifi.put("channel", channel);
            wifi.put("ssid", Base64.encodeToString(
                    ssid.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
            return wifi;
        }
    }

    public static List<WifiEntry> parseWifiList(JSONObject payload) throws Exception {
        List<WifiEntry> result = new ArrayList<>();
        JSONArray array = payload.getJSONArray("wifiList");
        for (int i = 0; i < array.length(); i++) {
            JSONObject wifi = array.getJSONObject(i);
            String ssid = new String(Base64.decode(wifi.getString("ssid"), Base64.NO_WRAP),
                    StandardCharsets.UTF_8);
            if (ssid == null || ssid.trim().isEmpty()) {
                continue;
            }
            result.add(new WifiEntry(
                    ssid,
                    wifi.getString("bssid"),
                    wifi.getInt("channel"),
                    wifi.optInt("signal", 0),
                    wifi.getInt("encryption"),
                    wifi.getInt("cipher")));
        }
        return result;
    }
}
