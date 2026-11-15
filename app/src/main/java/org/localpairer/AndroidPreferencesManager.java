package org.localpairer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.localpairer.model.MqttConfiguration;
import org.localpairer.merosslib.Utils;
import org.localpairer.merosslib.model.http.ApiCredentials;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import de.adorsys.android.securestoragelibrary.SecurePreferences;

import static org.localpairer.Constants.LOG_TAG;

public class AndroidPreferencesManager {
    private static String PREFS_CONFS = "org.localpairer.shared_preferences";
    private static String PREFS_WIFI_CREDS = "org.localpairer.shared_preferences.wifi_creds";
    private static String KEY_MQTT_CONF = "mqtt";
    private static String KEY_HTTP_CONF = "http";
    private static Gson g = Utils.getGson();

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
        String json = g.toJson(httpCredentials);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                SecurePreferences.setValue(KEY_HTTP_CONF, json, c);
                return;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Secure credential storage unavailable, falling back to SharedPreferences.", e);
            }
        }
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        settings.edit().putString(KEY_HTTP_CONF, json).apply();
    }

    public static void removeHttpCredentials(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                SecurePreferences.setValue(KEY_HTTP_CONF, "", c);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to remove secure HTTP credentials.", e);
            }
        }
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        settings.edit().remove(KEY_HTTP_CONF).apply();
    }

    @Nullable
    public static String getWifiStoredPassword(@NonNull Context c, @NonNull String bssid) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return SecurePreferences.getStringValue(bssid.trim().toLowerCase(), c, null);
        } else {
            return c.getSharedPreferences(PREFS_WIFI_CREDS, Context.MODE_PRIVATE).getString(bssid.trim().toLowerCase(), null);
        }
    }

    public static void storeWifiStoredPassword(@NonNull Context c, @NonNull String bssid, @NonNull String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            SecurePreferences.setValue(bssid.trim().toLowerCase(), password, c);
        } else {
            SharedPreferences.Editor e = c.getSharedPreferences(PREFS_WIFI_CREDS, Context.MODE_PRIVATE).edit();
            e.putString(bssid.trim().toLowerCase(), password);
            e.apply();
        }
    }

    @Nullable
    public static ApiCredentials loadHttpCredentials(Context c) {
        String json = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                json = SecurePreferences.getStringValue(KEY_HTTP_CONF, c, null);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to read secure HTTP credentials.", e);
            }
        }
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
