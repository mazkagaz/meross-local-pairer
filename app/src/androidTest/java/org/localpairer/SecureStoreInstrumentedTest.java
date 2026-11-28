package org.localpairer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SecureStoreInstrumentedTest {

    @Test
    public void putGetRemoveRoundTrip() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String key = "instrumented_secure_store_test";

        SecureStore.put(context, key, "secret-value");
        assertEquals("secret-value", SecureStore.get(context, key));

        SharedPreferences raw = context.getSharedPreferences(
                "org.localpairer.secure_prefs_v3", Context.MODE_PRIVATE);
        String ciphertext = raw.getString(key, null);
        assertNotNull(ciphertext);
        assertFalse(ciphertext.contains("secret-value"));

        SecureStore.remove(context, key);
        assertNull(SecureStore.get(context, key));
    }
}
