package org.localpairer;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores the SHA-256 fingerprint of the broker TLS certificate the user
 * accepted, keyed by host and port.
 */
public final class TlsPinStore {
    private static final String PREFS = "org.localpairer.shared_preferences";
    private static final String KEY_PREFIX = "tls_pin_";

    private TlsPinStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String get(Context context, String hostKey) {
        String value = prefs(context).getString(KEY_PREFIX + hostKey, null);
        return value == null || value.isEmpty() ? null : value;
    }

    public static void store(Context context, String hostKey, String sha256) {
        prefs(context).edit().putString(KEY_PREFIX + hostKey, sha256).apply();
    }

    public static void clear(Context context, String hostKey) {
        prefs(context).edit().remove(KEY_PREFIX + hostKey).apply();
    }

    public static void clearAll(Context context) {
        SharedPreferences preferences = prefs(context);
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) {
            if (key.startsWith(KEY_PREFIX)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }
}
