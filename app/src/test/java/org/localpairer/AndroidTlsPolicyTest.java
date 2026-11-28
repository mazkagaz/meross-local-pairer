package org.localpairer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AndroidTlsPolicyTest {

    @Test
    public void confirmedFingerprintIsPinnedAndCanBeCleared() {
        Context context = RuntimeEnvironment.getApplication();
        String url = "https://broker.local:2004";

        AndroidTlsPolicy.pinFingerprint(context, url, "abc123");
        assertEquals("abc123", AndroidTlsPolicy.pinnedFingerprint(context, url));

        AndroidTlsPolicy.clearPin(context, url);
        assertNull(AndroidTlsPolicy.pinnedFingerprint(context, url));
    }
}
