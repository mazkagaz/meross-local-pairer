package org.localpairer;

import android.app.AlertDialog;
import android.content.Context;

/**
 * Asks the user to accept a broker certificate that no longer matches the
 * pinned fingerprint, after showing its description.
 */
public final class TlsPinMismatchDialog {

    public interface RetryAction {
        void retry();
    }

    private TlsPinMismatchDialog() {
    }

    /**
     * Shows the dialog when {@code error} is a TLS pin mismatch.
     *
     * @return true when a dialog was shown.
     */
    public static boolean show(Context context, String apiServerUrl,
                               Throwable error, RetryAction retry) {
        if (context == null || apiServerUrl == null || retry == null) {
            return false;
        }
        if (AndroidTlsPolicy.pinMismatchMessage(error) == null) {
            return false;
        }

        String description = AndroidTlsPolicy.mismatchDescription(apiServerUrl);
        String pinned = AndroidTlsPolicy.pinnedFingerprint(context, apiServerUrl);
        StringBuilder message = new StringBuilder(
                "The broker presented a different TLS certificate than the one "
                        + "previously accepted.\n\n");
        message.append(description);
        if (pinned != null) {
            message.append("\n\nPreviously pinned SHA-256:\n").append(pinned);
        }
        message.append("\n\nAccept the new certificate and continue?");

        new AlertDialog.Builder(context)
                .setTitle("Broker certificate changed")
                .setMessage(message.toString())
                .setPositiveButton("Accept", (dialog, which) -> {
                    AndroidTlsPolicy.clearPin(context, apiServerUrl);
                    PairingLog.log("tls", "accepted new certificate for " + apiServerUrl);
                    retry.retry();
                })
                .setNegativeButton("Deny", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
        return true;
    }
}
