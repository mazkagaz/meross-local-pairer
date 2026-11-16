package org.localpairer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** User-validated Wi-Fi networks on which broker traffic is allowed. */
public final class AllowedWifiNetworks {
    public static final class Entry {
        public final String ssid;
        public final String bssid;

        public Entry(String ssid, String bssid) {
            this.ssid = ssid;
            this.bssid = bssid;
        }

        public String label() {
            return bssid != null && !bssid.isEmpty() ? ssid + " (" + bssid + ")" : ssid;
        }
    }

    private static final String PREFS = "org.localpairer.shared_preferences";
    private static final String KEY = "allowed_wifi_networks";

    private AllowedWifiNetworks() {
    }

    public static List<Entry> load(Context context) {
        List<Entry> result = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                result.add(new Entry(object.getString("ssid"), object.optString("bssid", "")));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private static void save(Context context, List<Entry> entries) {
        JSONArray array = new JSONArray();
        try {
            for (Entry entry : entries) {
                JSONObject object = new JSONObject();
                object.put("ssid", entry.ssid);
                object.put("bssid", entry.bssid != null ? entry.bssid : "");
                array.put(object);
            }
        } catch (Exception ignored) {
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, array.toString()).apply();
    }

    public static void add(Context context, String ssid, String bssid) {
        if (ssid == null || ssid.trim().isEmpty()) {
            return;
        }
        List<Entry> entries = load(context);
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (entry.ssid.equals(ssid)) {
                // Refresh the BSSID if the access point changed.
                entries.remove(i);
                break;
            }
        }
        entries.add(new Entry(ssid, bssid));
        save(context, entries);
    }

    public static void remove(Context context, Entry target) {
        List<Entry> entries = load(context);
        java.util.Iterator<Entry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.ssid.equals(target.ssid)
                    && (entry.bssid == null ? "" : entry.bssid)
                    .equalsIgnoreCase(target.bssid == null ? "" : target.bssid)) {
                iterator.remove();
            }
        }
        save(context, entries);
    }

    public static boolean isAllowed(Context context, String ssid, String bssid) {
        if (ssid == null) {
            return false;
        }
        for (Entry entry : load(context)) {
            if (!entry.ssid.equals(ssid)) {
                continue;
            }
            if (entry.bssid == null || entry.bssid.isEmpty()) {
                return true;
            }
            if (bssid == null || bssid.isEmpty()
                    || "02:00:00:00:00:00".equals(bssid)) {
                // The OS did not expose a usable BSSID; the SSID is enough.
                return true;
            }
            return entry.bssid.equalsIgnoreCase(bssid);
        }
        return false;
    }

    public static Entry current(Context context) {
        WifiManager manager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (manager == null || manager.getConnectionInfo() == null) {
            return null;
        }
        WifiInfo info = manager.getConnectionInfo();
        String ssid = info.getSSID();
        if (ssid == null) {
            return null;
        }
        if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() >= 2) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        if (ssid.isEmpty() || ssid.contains("unknown") || ssid.contains("UNKNOWN")) {
            return null;
        }
        String bssid = info.getBSSID();
        if ("02:00:00:00:00:00".equals(bssid)) {
            bssid = null;
        }
        return new Entry(ssid, bssid);
    }

    public static boolean isCurrentAllowed(Context context, boolean allowDeviceAp) {
        Entry current = current(context);
        if (current == null) {
            return false;
        }
        if (allowDeviceAp && MerossUtils.isMerossAp(current.ssid)) {
            return true;
        }
        return isAllowed(context, current.ssid, current.bssid);
    }
}
