package org.localpairer;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Bounded in-memory log for pairing diagnostics. Always mirrored to logcat;
 * only retained for the copy-to-clipboard action when the user enabled it.
 */
public final class PairingLog {
    private static final String LOG_TAG = "MerossPairing";
    private static final int MAX_LINES = 2000;
    private static final List<String> LINES = new ArrayList<>();
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private static volatile boolean enabled = false;

    private PairingLog() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void log(String area, String message) {
        String line;
        synchronized (TIME_FORMAT) {
            line = TIME_FORMAT.format(new Date()) + " [" + area + "] " + message;
        }
        Log.d(LOG_TAG, line);
        if (!enabled) {
            return;
        }
        synchronized (LINES) {
            LINES.add(line);
            while (LINES.size() > MAX_LINES) {
                LINES.remove(0);
            }
        }
    }

    public static String dump() {
        StringBuilder builder = new StringBuilder();
        builder.append("Meross Local Pairer ").append(BuildConfig.VERSION_NAME)
                .append(" - pairing log\n");
        synchronized (LINES) {
            for (String line : LINES) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    public static void clear() {
        synchronized (LINES) {
            LINES.clear();
        }
    }
}
