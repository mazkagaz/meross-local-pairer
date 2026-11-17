package org.localpairer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Map;

import javax.crypto.Cipher;

/**
 * Keystore-backed storage for credentials and Wi-Fi passwords.
 *
 * Values previously written by the abandoned adorsys SecureStorage library are
 * decrypted once with its legacy RSA key and migrated into the new store.
 */
final class SecureStore {
    private static final String TAG = "SecureStore";
    private static final String FILE = "org.localpairer.secure_prefs_v2";
    private static final String LEGACY_FILE = "SecurePreferences";
    private static final String LEGACY_ALIAS = "adorsysKeyPair";
    private static final String MIGRATED = "legacy_migrated";

    private static SharedPreferences preferences;
    private static boolean unavailable;

    private SecureStore() {
    }

    static synchronized String get(Context context, String key) {
        SharedPreferences store = open(context);
        return store == null ? null : store.getString(key, null);
    }

    static synchronized void put(Context context, String key, String value) {
        SharedPreferences store = open(context);
        if (store == null) {
            Log.e(TAG, "Secure storage unavailable; refusing to persist a secret.");
            return;
        }
        store.edit().putString(key, value).apply();
    }

    static synchronized void remove(Context context, String key) {
        SharedPreferences store = open(context);
        if (store != null) {
            store.edit().remove(key).apply();
        }
    }

    private static SharedPreferences open(Context context) {
        if (preferences != null) {
            return preferences;
        }
        if (unavailable) {
            return null;
        }
        Context appContext = context.getApplicationContext();
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            preferences = EncryptedSharedPreferences.create(
                    appContext,
                    FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            try {
                migrateLegacy(appContext, preferences);
            } catch (Exception e) {
                Log.w(TAG, "Legacy secure storage migration did not complete.");
            }
            return preferences;
        } catch (Exception e) {
            // Fail closed: callers see no secret rather than a plaintext fallback.
            Log.e(TAG, "Could not open encrypted preferences.", e);
            unavailable = true;
            return null;
        }
    }

    private static void migrateLegacy(Context context, SharedPreferences store) {
        if (store.getBoolean(MIGRATED, false)) {
            return;
        }
        SharedPreferences legacy = context.getSharedPreferences(LEGACY_FILE, Context.MODE_PRIVATE);
        Map<String, ?> entries = legacy.getAll();
        SharedPreferences.Editor editor = store.edit();
        boolean failed = false;
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof String) || ((String) value).isEmpty()
                    || store.contains(entry.getKey())) {
                continue;
            }
            try {
                editor.putString(entry.getKey(), decryptLegacy((String) value));
            } catch (Exception e) {
                failed = true;
                Log.w(TAG, "Could not migrate one legacy secure value.");
            }
        }
        if (!failed) {
            editor.putBoolean(MIGRATED, true);
        }
        editor.apply();
        if (!failed) {
            legacy.edit().clear().apply();
        }
    }

    private static String decryptLegacy(String value) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(LEGACY_ALIAS, null);
        if (privateKey == null) {
            throw new IllegalStateException("Legacy keystore key is missing");
        }
        Cipher cipher = legacyCipher();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] plain = cipher.doFinal(Base64.decode(value, Base64.DEFAULT));
        return new String(plain, StandardCharsets.UTF_8);
    }

    private static Cipher legacyCipher() throws Exception {
        try {
            return Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidKeyStoreBCWorkaround");
        } catch (Exception ignored) {
            return Cipher.getInstance("RSA/ECB/PKCS1Padding");
        }
    }
}
