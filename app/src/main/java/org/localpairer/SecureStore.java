package org.localpairer;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Keystore-backed storage for credentials and Wi-Fi passwords.
 *
 * Values are encrypted with an AES-256-GCM key that never leaves Android
 * Keystore. The old AndroidX EncryptedSharedPreferences file is read once to
 * migrate existing installations; AndroidX security-crypto is only needed for
 * that migration and must not be used for new values.
 */
final class SecureStore {
    private static final String TAG = "SecureStore";
    private static final String FILE = "org.localpairer.secure_prefs_v3";
    private static final String LEGACY_FILE = "SecurePreferences";
    private static final String LEGACY_ALIAS = "adorsysKeyPair";
    private static final String ENCRYPTED_FILE = "org.localpairer.secure_prefs_v2";
    private static final String KEY_ALIAS = "localpairer_secret_store";
    private static final String MIGRATED = "legacy_migrated";
    private static final String ENCRYPTED_MIGRATED = "encrypted_v2_migrated";

    private static SharedPreferences preferences;

    private SecureStore() {
    }

    static synchronized String get(Context context, String key) {
        SharedPreferences store = open(context);
        if (store == null) {
            return null;
        }
        String encrypted = store.getString(key, null);
        if (encrypted == null) {
            return null;
        }
        try {
            return decrypt(key, encrypted);
        } catch (Exception e) {
            Log.e(TAG, "Could not decrypt a stored secret.", e);
            return null;
        }
    }

    static synchronized boolean put(Context context, String key, String value) {
        SharedPreferences store = open(context);
        if (store == null) {
            Log.e(TAG, "Secure storage unavailable; refusing to persist a secret.");
            return false;
        }
        try {
            return store.edit().putString(key, encrypt(key, value)).commit();
        } catch (Exception e) {
            Log.e(TAG, "Could not encrypt a secret.", e);
            return false;
        }
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
        Context appContext = context.getApplicationContext();
        SharedPreferences store = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        try {
            migrateEncryptedPreferences(appContext, store);
            migrateAdorsysPreferences(appContext, store);
            preferences = store;
            return preferences;
        } catch (Exception e) {
            Log.e(TAG, "Could not open encrypted preferences.", e);
            return null;
        }
    }

    /** Reads the AndroidX EncryptedSharedPreferences file once. */
    private static void migrateEncryptedPreferences(Context context,
                                                     SharedPreferences store) {
        if (store.getBoolean(ENCRYPTED_MIGRATED, false)) {
            return;
        }
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            SharedPreferences old = EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            SharedPreferences.Editor editor = store.edit();
            boolean failed = false;
            for (Map.Entry<String, ?> entry : old.getAll().entrySet()) {
                if (entry.getKey().startsWith("__androidx_security_crypto")
                        || store.contains(entry.getKey())
                        || !(entry.getValue() instanceof String)) {
                    continue;
                }
                try {
                    editor.putString(entry.getKey(),
                            encrypt(entry.getKey(), (String) entry.getValue()));
                } catch (Exception e) {
                    failed = true;
                    Log.w(TAG, "Could not migrate one encrypted secure value.");
                }
            }
            if (!failed) {
                editor.putBoolean(ENCRYPTED_MIGRATED, true);
            }
            boolean committed = editor.commit();
            if (!failed && committed) {
                old.edit().clear().apply();
            }
        } catch (Exception e) {
            // No old encrypted file is normal on a fresh install.
            Log.i(TAG, "No AndroidX encrypted preferences to migrate.");
        }
    }

    /** Migrates values from the abandoned adorsys SecureStorage library. */
    private static void migrateAdorsysPreferences(Context context,
                                                   SharedPreferences store) {
        if (store.getBoolean(MIGRATED, false)) {
            return;
        }
        SharedPreferences legacy = context.getSharedPreferences(LEGACY_FILE, Context.MODE_PRIVATE);
        Map<String, ?> entries = legacy.getAll();
        if (entries.isEmpty()) {
            store.edit().putBoolean(MIGRATED, true).apply();
            return;
        }
        SharedPreferences.Editor editor = store.edit();
        boolean failed = false;
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof String) || ((String) value).isEmpty()
                    || store.contains(entry.getKey())) {
                continue;
            }
            try {
                editor.putString(entry.getKey(),
                        encrypt(entry.getKey(), decryptLegacy((String) value)));
            } catch (Exception e) {
                failed = true;
                Log.w(TAG, "Could not migrate one legacy secure value.");
            }
        }
        if (!failed) {
            editor.putBoolean(MIGRATED, true);
        }
        boolean committed = editor.commit();
        if (!failed && committed) {
            legacy.edit().clear().apply();
        }
    }

    private static String encrypt(String key, String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey());
        cipher.updateAAD(key.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":"
                + Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    private static String decrypt(String key, String value) throws Exception {
        String[] parts = value.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Malformed encrypted value");
        }
        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(128, iv));
        cipher.updateAAD(key.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private static SecretKey secretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            generator.generateKey();
        }
        try {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        } catch (UnrecoverableKeyException e) {
            throw new IllegalStateException("Secret store key is unavailable", e);
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
