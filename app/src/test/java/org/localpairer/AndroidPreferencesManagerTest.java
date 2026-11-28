package org.localpairer;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.localpairer.model.MqttConfiguration;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AndroidPreferencesManagerTest {

    @Test
    public void replacingAnExistingMqttConfigurationDoesNotThrow() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("org.localpairer.shared_preferences",
                Context.MODE_PRIVATE).edit().clear().commit();

        AndroidPreferencesManager.storeNewMqttConfiguration(
                context, new MqttConfiguration("broker", "old", 2001));
        AndroidPreferencesManager.storeNewMqttConfiguration(
                context, new MqttConfiguration("other", "other", 2001));
        AndroidPreferencesManager.storeNewMqttConfiguration(
                context, new MqttConfiguration("broker", "new", 2001));

        assertEquals(2,
                AndroidPreferencesManager.loadAllMqttConfigurations(context).size());
        assertEquals("new",
                AndroidPreferencesManager.loadAllMqttConfigurations(context)
                        .get(1).getHostname());
    }
}
