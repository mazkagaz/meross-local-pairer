package org.localpairer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppUpdatePolicyTest {

    @Test
    public void fdroidInstallersDisableUpdates() {
        assertTrue(AppUpdatePolicy.isFdroidInstaller("org.fdroid.fdroid"));
        assertTrue(AppUpdatePolicy.isFdroidInstaller("org.fdroid.basic"));
        assertFalse(AppUpdatePolicy.isFdroidInstaller("com.android.vending"));
        assertFalse(AppUpdatePolicy.isFdroidInstaller(null));
        assertTrue(AppUpdatePolicy.updatesDisabled(true,
                "com.android.vending"));
        assertTrue(AppUpdatePolicy.updatesDisabled(false,
                "org.fdroid.fdroid"));
        assertFalse(AppUpdatePolicy.updatesDisabled(false, null));
    }

    @Test
    public void tagsAreNormalized() {
        assertEquals("2.1.0", AppUpdatePolicy.normalizeTag("v2.1.0"));
        assertEquals("2.1.0", AppUpdatePolicy.normalizeTag("V2.1.0"));
        assertEquals("2.1.0", AppUpdatePolicy.normalizeTag(" 2.1.0 "));
        assertEquals(null, AppUpdatePolicy.normalizeTag(null));
    }

    @Test
    public void newerVersionsAreDetected() {
        assertTrue(AppUpdatePolicy.isNewer("2.0.9", "v2.1.0"));
        assertTrue(AppUpdatePolicy.isNewer("2.0.9", "2.0.10"));
        assertTrue(AppUpdatePolicy.isNewer("2.0", "2.0.1"));
        assertFalse(AppUpdatePolicy.isNewer("2.1.0", "v2.0.9"));
        assertFalse(AppUpdatePolicy.isNewer("2.1.0", "2.1.0"));
        assertFalse(AppUpdatePolicy.isNewer("2.1.0", "2.1"));
    }

    @Test
    public void malformedVersionsNeverTriggerAnUpdate() {
        assertFalse(AppUpdatePolicy.isNewer("2.0.9", null));
        assertFalse(AppUpdatePolicy.isNewer("2.0.9", ""));
        assertFalse(AppUpdatePolicy.isNewer("2.0.9", "latest"));
        assertFalse(AppUpdatePolicy.isNewer("2.0.9", "v2.1.0-beta.1"));
        assertFalse(AppUpdatePolicy.isNewer("", "2.1.0"));
        assertFalse(AppUpdatePolicy.isNewer(null, "2.1.0"));
    }

    @Test
    public void preReleaseSuffixIsNotComparedAsNewer() {
        assertFalse(AppUpdatePolicy.isNewer("2.0.9", "2.0.9-beta.1"));
        assertFalse(AppUpdatePolicy.isNewer("2.0.9", "v2.0.9-rc1"));
    }
}
