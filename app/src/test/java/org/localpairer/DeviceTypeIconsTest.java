package org.localpairer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DeviceTypeIconsTest {
    @Test
    public void rollerShutterTypesAreRecognizedCaseInsensitively() {
        assertTrue(DeviceTypeIcons.isRollerShutter("MRS100"));
        assertTrue(DeviceTypeIcons.isRollerShutter("mss560"));
    }

    @Test
    public void otherDevicesUseTheGenericPath() {
        assertFalse(DeviceTypeIcons.isRollerShutter("msl610"));
        assertFalse(DeviceTypeIcons.isRollerShutter(""));
        assertFalse(DeviceTypeIcons.isRollerShutter(null));
    }
}
