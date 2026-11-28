package org.localpairer;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AppLaunchTest {

    @Test
    public void splashActivityInflatesItsLayout() {
        ActivityController<SplashActivity> controller =
                Robolectric.buildActivity(SplashActivity.class).setup();
        try {
            SplashActivity activity = controller.get();
            assertNotNull(activity.findViewById(R.id.acceptButton));
            assertNotNull(activity.findViewById(R.id.quitButton));
            assertNotNull(activity.findViewById(R.id.textView29));
        } finally {
            controller.pause().stop().destroy();
        }
    }

    @Test
    public void mainActivityInflatesItsLayout() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        try {
            MainActivity activity = controller.get();
            assertNotNull(activity.findViewById(R.id.toolbar));
            assertNotNull(activity.findViewById(R.id.nav_host_fragment));
        } finally {
            controller.pause().stop().destroy();
        }
    }
}
