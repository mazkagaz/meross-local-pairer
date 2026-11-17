package org.localpairer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.util.HashSet;
import java.util.Set;

/**
 * Offers, once per network and process, to trust the Wi-Fi network the phone
 * is currently connected to. Declined networks are remembered so the prompt
 * does not nag on later launches; trusted networks are managed from the
 * advanced settings.
 */
public final class WifiTrustPrompt {
    private static final int REQUEST_CODE = 51;
    private static final Set<String> promptedSsids = new HashSet<>();
    private static boolean permissionRequested;

    private WifiTrustPrompt() {
    }

    public static void maybePrompt(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (!hasPermissions(activity)) {
            if (!permissionRequested) {
                permissionRequested = true;
                ActivityCompat.requestPermissions(activity, permissions(), REQUEST_CODE);
            }
            return;
        }
        if (!AndroidUtils.isLocationEnabled(activity)) {
            // Android hides SSIDs without Location services. The settings
            // screen explains this when the user explicitly asks.
            return;
        }
        AllowedWifiNetworks.Entry current = AllowedWifiNetworks.current(activity);
        if (current == null || MerossUtils.isMerossAp(current.ssid)) {
            return;
        }
        if (AllowedWifiNetworks.isAllowed(activity, current.ssid, current.bssid)
                || AndroidPreferencesManager.isWifiDeclined(activity, current.ssid)) {
            return;
        }
        if (promptedSsids.contains(current.ssid)) {
            return;
        }
        promptedSsids.add(current.ssid);
        show(activity, current);
    }

    public static void onPermissionsResult(Activity activity) {
        maybePrompt(activity);
    }

    private static void show(Activity activity, AllowedWifiNetworks.Entry network) {
        new AlertDialog.Builder(activity)
                .setTitle("Trust this Wi-Fi network?")
                .setMessage("Broker traffic (HTTP/MQTT) will be allowed on \""
                        + network.ssid + "\". Accept only your home network.")
                .setPositiveButton("Trust", (dialog, which) -> {
                    AllowedWifiNetworks.add(activity, network.ssid, network.bssid);
                    AndroidPreferencesManager.clearWifiDeclined(activity, network.ssid);
                    PairingLog.log("network", "trusted at startup " + network.ssid);
                    Toast.makeText(activity, "Trusted " + network.ssid + ".",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Not now", (dialog, which) -> {
                    AndroidPreferencesManager.markWifiDeclined(activity, network.ssid);
                    PairingLog.log("network", "declined at startup " + network.ssid);
                })
                .setOnCancelListener(dialog ->
                        AndroidPreferencesManager.markWifiDeclined(activity, network.ssid))
                .show();
    }

    private static String[] permissions() {
        if (Build.VERSION.SDK_INT >= 37) {
            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.ACCESS_LOCAL_NETWORK};
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES};
        }
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    private static boolean hasPermissions(Activity activity) {
        for (String permission : permissions()) {
            if (ActivityCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
