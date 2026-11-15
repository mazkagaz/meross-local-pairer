package org.localpairer;

import androidx.annotation.Nullable;

public class MerossUtils {
    public static boolean isMerossAp(@Nullable String ssid) {
        if (ssid == null)
            return false;
        // Prefix families (AP, FC, H0, HP, LB, RS, ...) followed by a
        // 4+ character suffix. Also accepts variants such as
        // Meross_RS_chWF_1234 seen in MRS100 factory/test firmware.
        return ssid.trim().matches("^Meross_[A-Za-z0-9]{2,}_.+$");
    }
}
