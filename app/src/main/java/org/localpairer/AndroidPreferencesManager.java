package org.localpairer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.localpairer.model.MqttConfiguration;
import org.localpairer.merosslib.Utils;
import org.localpairer.merosslib.model.http.ApiCredentials;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import static org.localpairer.Constants.LOG_TAG;

public class AndroidPreferencesManager {
    private static String PREFS_CONFS = "org.localpairer.shared_preferences";
    private static String KEY_MQTT_CONF = "mqtt";
    private static String KEY_HTTP_CONF = "http";
    private static String KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted";
    private static String KEY_PAIRING_DEBUG = "pairing_debug";
    private static String KEY_BROKER_HOST = "broker_host";
    private static String KEY_BROKER_HTTPS = "broker_use_https";
    private static String KEY_API_HTTP_PORT = "api_http_port";
    private static String KEY_API_HTTPS_PORT = "api_https_port";
    private static String KEY_ADVANCED_MODE = "advanced_mode";
    private static String KEY_DECLINED_WIFI = "declined_wifi_networks";
    private static String KEY_LAST_DEVICE_WIFI_SSID = "last_device_wifi_ssid";
    private static String KEY_LAST_DEVICE_WIFI_BSSID = "last_device_wifi_bssid";
    private static Gson g = Utils.getGson();

    public static boolean isPairingDebugEnabled(Context c) {
        return c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_PAIRING_DEBUG, false);
    }

    public static void setPairingDebugEnabled(Context c, boolean enabled) {
        c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_PAIRING_DEBUG, enabled).apply();
    }

    /**
     * Loads the broker endpoint shown by the simple setup UI. On first use it
     * migrates the full URL from stored credentials; a non-default port keeps
     * itself in the matching port field and opens the advanced mode.
     */
    public static BrokerEndpoint loadBrokerEndpoint(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        String host = settings.getString(KEY_BROKER_HOST, null);
        boolean https = settings.getBoolean(KEY_BROKER_HTTPS, true);
        int httpPort = settings.getInt(KEY_API_HTTP_PORT, BrokerEndpoint.DEFAULT_HTTP_PORT);
        int httpsPort = settings.getInt(KEY_API_HTTPS_PORT, BrokerEndpoint.DEFAULT_HTTPS_PORT);

        if (host == null) {
            ApiCredentials credentials = loadHttpCredentials(c);
            if (credentials != null && credentials.getApiServer() != null) {
                BrokerEndpoint migrated = BrokerEndpoint.fromUrl(credentials.getApiServer());
                if (migrated != null) {
                    host = migrated.getHost();
                    https = migrated.isHttps();
                    boolean customPort = false;
                    if (https && migrated.getHttpsPort() != BrokerEndpoint.DEFAULT_HTTPS_PORT) {
                        httpsPort = migrated.getHttpsPort();
                        customPort = true;
                    } else if (!https && migrated.getHttpPort() != BrokerEndpoint.DEFAULT_HTTP_PORT) {
                        httpPort = migrated.getHttpPort();
                        customPort = true;
                    }
                    saveBrokerEndpoint(c, new BrokerEndpoint(host, https, httpPort, httpsPort));
                    if (customPort) {
                        setAdvancedMode(c, true);
                    }
                }
            } else if (credentials != null && credentials.isManuallySet()) {
                // Manual credentials carry no endpoint: technical by nature.
                setAdvancedMode(c, true);
            }
        }
        return new BrokerEndpoint(host, https, httpPort, httpsPort);
    }

    public static void saveBrokerEndpoint(Context c, BrokerEndpoint endpoint) {
        c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BROKER_HOST, endpoint.getHost())
                .putBoolean(KEY_BROKER_HTTPS, endpoint.isHttps())
                .putInt(KEY_API_HTTP_PORT, endpoint.getHttpPort())
                .putInt(KEY_API_HTTPS_PORT, endpoint.getHttpsPort())
                .apply();
    }

    public static boolean isAdvancedMode(Context c) {
        return c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ADVANCED_MODE, false);
    }

    public static void setAdvancedMode(Context c, boolean enabled) {
        c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ADVANCED_MODE, enabled).apply();
    }

    public static boolean isWifiDeclined(Context c, String ssid) {
        if (ssid == null) {
            return false;
        }
        return c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE)
                .getStringSet(KEY_DECLINED_WIFI, new java.util.HashSet<String>())
                .contains(ssid);
    }

    public static void markWifiDeclined(Context c, String ssid) {
        if (ssid == null) {
            return;
        }
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        java.util.Set<String> declined =
                new java.util.HashSet<>(settings.getStringSet(KEY_DECLINED_WIFI,
                        new java.util.HashSet<String>()));
        declined.add(ssid);
        settings.edit().putStringSet(KEY_DECLINED_WIFI, declined).apply();
    }

    public static void clearWifiDeclined(Context c, String ssid) {
        if (ssid == null) {
            return;
        }
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        java.util.Set<String> declined =
                new java.util.HashSet<>(settings.getStringSet(KEY_DECLINED_WIFI,
                        new java.util.HashSet<String>()));
        if (declined.remove(ssid)) {
            settings.edit().putStringSet(KEY_DECLINED_WIFI, declined).apply();
        }
    }

    public static void storeLastDeviceWifi(Context c, @Nullable String ssid,
                                           @Nullable String bssid) {
        if (c == null || ssid == null || ssid.trim().isEmpty()) {
            return;
        }
        c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_DEVICE_WIFI_SSID, ssid)
                .putString(KEY_LAST_DEVICE_WIFI_BSSID,
                        bssid != null ? bssid : "")
                .apply();
    }

    @Nullable
    public static AllowedWifiNetworks.Entry loadLastDeviceWifi(@Nullable Context c) {
        if (c == null) {
            return null;
        }
        SharedPreferences settings =
                c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        String ssid = settings.getString(KEY_LAST_DEVICE_WIFI_SSID, null);
        if (ssid == null || ssid.trim().isEmpty()) {
            return null;
        }
        return new AllowedWifiNetworks.Entry(
                ssid, settings.getString(KEY_LAST_DEVICE_WIFI_BSSID, ""));
    }

    public static boolean isDisclaimerAccepted(Context c) {
        return c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DISCLAIMER_ACCEPTED, false);
    }

    public static void setDisclaimerAccepted(Context c, boolean accepted) {
        c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_DISCLAIMER_ACCEPTED, accepted).apply();
    }

    public static void storeNewMqttConfiguration(Context context, MqttConfiguration conf) {
        List<MqttConfiguration> allConfs = loadAllMqttConfigurations(context);
        for (MqttConfiguration c : allConfs) {
            if (c.getName().toLowerCase().trim().compareTo(conf.getName().toLowerCase().trim())==0) {
                allConfs.remove(c);
            }
        }
        allConfs.add(conf);

        SharedPreferences settings = context.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String json = g.toJson(allConfs);
        editor.putString(KEY_MQTT_CONF, json);
        editor.apply();
    }

    public static List<MqttConfiguration> loadAllMqttConfigurations(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        List<MqttConfiguration> res = null;
        try {
            res = g.fromJson(settings.getString(KEY_MQTT_CONF, null), new TypeToken<List<MqttConfiguration>>() {}.getType());
        } catch (Exception e) {
            // Error while loading configurations
            Log.e(LOG_TAG, "Error while loading stored MQTT Configurations. This error is ignored.");
        }
        if (res == null)
            return new ArrayList<>();
        else
            return res;

    }

    public static void storeHttpCredentials(Context c, ApiCredentials httpCredentials) {
        SecureStore.put(c, KEY_HTTP_CONF, g.toJson(httpCredentials));
    }

    public static void removeHttpCredentials(Context c) {
        SecureStore.remove(c, KEY_HTTP_CONF);
        // Remove a legacy plaintext copy left by old versions.
        c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_HTTP_CONF).apply();
    }

    @Nullable
    public static String getWifiStoredPassword(@NonNull Context c, @NonNull String bssid) {
        return SecureStore.get(c, bssid.trim().toLowerCase());
    }

    public static void storeWifiStoredPassword(@NonNull Context c, @NonNull String bssid, @NonNull String password) {
        SecureStore.put(c, bssid.trim().toLowerCase(), password);
    }

    /**
     * Finds a stored password for an SSID even when the BSSID changed (mesh
     * roaming) or the password was saved for another access point of the same
     * network.
     */
    @Nullable
    public static String getWifiStoredPasswordForNetwork(@Nullable Context c, @Nullable String ssid,
                                                         @Nullable String bssid) {
        if (c == null) {
            return null;
        }
        String value = null;
        if (bssid != null && !bssid.trim().isEmpty()) {
            value = getWifiStoredPassword(c, bssid);
        }
        if (value != null) {
            return value;
        }
        if (ssid == null || ssid.trim().isEmpty()) {
            return null;
        }
        value = getWifiStoredPassword(c, wifiPasswordKey(ssid));
        if (value != null) {
            return value;
        }
        for (AllowedWifiNetworks.Entry entry : AllowedWifiNetworks.load(c)) {
            if (entry.ssid != null && entry.ssid.equalsIgnoreCase(ssid)
                    && entry.bssid != null && !entry.bssid.trim().isEmpty()) {
                value = getWifiStoredPassword(c, entry.bssid);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    public static void storeWifiStoredPassword(@NonNull Context c, @Nullable String ssid,
                                               @Nullable String bssid, @NonNull String password) {
        if (bssid != null && !bssid.trim().isEmpty()) {
            storeWifiStoredPassword(c, bssid, password);
        }
        if (ssid != null && !ssid.trim().isEmpty()) {
            storeWifiStoredPassword(c, wifiPasswordKey(ssid), password);
        }
    }

    private static String wifiPasswordKey(String ssid) {
        return "ssid:" + ssid.trim().toLowerCase();
    }

    @Nullable
    public static ApiCredentials loadHttpCredentials(Context c) {
        String json = SecureStore.get(c, KEY_HTTP_CONF);
        if (json == null) {
            // Migrate legacy plaintext credentials on first read.
            SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
            json = settings.getString(KEY_HTTP_CONF, null);
            if (json != null) {
                ApiCredentials legacy = g.fromJson(json, ApiCredentials.class);
                if (legacy != null) {
                    storeHttpCredentials(c, legacy);
                }
                settings.edit().remove(KEY_HTTP_CONF).apply();
            }
        }
        return json == null ? null : g.fromJson(json, ApiCredentials.class);
    }
}
