package org.localpairer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Maps a Meross device type to the icon shown in the app. */
public final class DeviceTypeIcons {
    private static final Set<String> ROLLER_SHUTTER_TYPES =
            new HashSet<>(Arrays.asList("mrs100", "mss560", "mss570", "mrs200"));

    private DeviceTypeIcons() {
    }

    public static boolean isRollerShutter(String type) {
        return type != null
                && ROLLER_SHUTTER_TYPES.contains(type.toLowerCase(Locale.ROOT));
    }

    public static int iconFor(String type) {
        return isRollerShutter(type)
                ? R.drawable.ic_roller_shutter : R.drawable.ic_smart_plug;
    }
}
