package org.localpairer;

import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppLaunchInstrumentedTest {

    @Before
    public void resetDisclaimer() {
        Context context = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        AndroidPreferencesManager.setDisclaimerAccepted(context, false);
    }

    @Test
    public void splashActivityLaunches() {
        try (ActivityScenario<SplashActivity> scenario =
                     ActivityScenario.launch(SplashActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.acceptButton));
                assertNotNull(activity.findViewById(R.id.quitButton));
                assertNotNull(activity.findViewById(R.id.textView29));
            });
        }
    }

    @Test
    public void mainActivityLaunches() {
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.toolbar));
                assertNotNull(activity.findViewById(R.id.nav_host_fragment));
            });
        }
    }
}
