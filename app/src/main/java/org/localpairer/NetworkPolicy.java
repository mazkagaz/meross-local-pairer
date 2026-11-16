package org.localpairer;

import android.app.Activity;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

/**
 * Broker traffic is only started on a Wi-Fi network the user explicitly
 * validated. Device APs used during pairing are allowed implicitly.
 */
public final class NetworkPolicy {
    private NetworkPolicy() {
    }

    public static boolean isCurrentAllowed(Activity activity) {
        return AllowedWifiNetworks.isCurrentAllowed(activity, true);
    }

    public static void ensureCurrentAllowed(Activity activity, Runnable onAllowed) {
        if (isCurrentAllowed(activity)) {
            onAllowed.run();
            return;
        }
        AllowedWifiNetworks.Entry current = AllowedWifiNetworks.current(activity);
        if (current == null) {
            Toast.makeText(activity,
                    "Unable to read the current Wi-Fi network. Check Wi-Fi, Location "
                            + "services and Wi-Fi/Location permissions.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        prompt(activity, current, onAllowed);
    }

    public static void ensureAllowed(Activity activity, String ssid, String bssid,
                                     Runnable onAllowed) {
        if (ssid == null || MerossUtils.isMerossAp(ssid)
                || AllowedWifiNetworks.isAllowed(activity, ssid, bssid)) {
            onAllowed.run();
            return;
        }
        prompt(activity, new AllowedWifiNetworks.Entry(ssid, bssid), onAllowed);
    }

    private static void prompt(Activity activity, AllowedWifiNetworks.Entry network,
                               Runnable onAllowed) {
        new AlertDialog.Builder(activity)
                .setTitle("Untrusted Wi-Fi network")
                .setMessage("Broker traffic (HTTP/MQTT) is only allowed on validated "
                        + "networks.\n\nAllow \"" + network.ssid + "\""
                        + (network.bssid != null ? " (" + network.bssid + ")" : "")
                        + " for Meross Local Pairer?")
                .setPositiveButton("Allow this network", (dialog, which) -> {
                    AllowedWifiNetworks.add(activity, network.ssid, network.bssid);
                    PairingLog.log("network", "allowed " + network.ssid + "/" + network.bssid);
                    onAllowed.run();
                })
                .setNegativeButton("Cancel", (dialog, which) ->
                        PairingLog.log("network", "blocked " + network.ssid + "/" + network.bssid))
                .show();
    }
}
