package org.localpairer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional GitHub update hint, disabled for F-Droid builds/installations. */
public final class AppUpdateChecker {

    public static final String RELEASES_URL =
            "https://github.com/mazkagaz/meross-local-pairer/releases/latest";
    private static final String API_URL =
            "https://api.github.com/repos/mazkagaz/meross-local-pairer/releases/latest";
    private static final String PREFS = "app_updates";
    private static final String LAST_NOTIFIED = "last_notified_version";
    private static final AtomicBoolean CHECKED = new AtomicBoolean(false);
    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppUpdateChecker() {
    }

    public static boolean isUpdateCheckDisabled(Context context) {
        return AppUpdatePolicy.updatesDisabled(
                BuildConfig.FDROID_BUILD, installerPackage(context));
    }

    public static void checkOnStartup(Activity activity) {
        if (isUpdateCheckDisabled(activity) || !CHECKED.compareAndSet(false, true)) {
            return;
        }
        EXECUTOR.execute(() -> {
            String tag = fetchLatestTag();
            if (tag == null) {
                return;
            }
            String latest = AppUpdatePolicy.normalizeTag(tag);
            if (!AppUpdatePolicy.isNewer(BuildConfig.VERSION_NAME, latest)) {
                return;
            }
            MAIN.post(() -> showIfNeeded(activity, latest));
        });
    }

    public static void openReleasesPage(Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(RELEASES_URL)));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(context, R.string.update_no_browser,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static void showIfNeeded(Activity activity, String latest) {
        if (activity.isFinishing()
                || (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) {
            return;
        }
        SharedPreferences prefs = activity.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        if (latest.equals(prefs.getString(LAST_NOTIFIED, null))) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.update_available_title)
                .setMessage(activity.getString(
                        R.string.update_available_message,
                        latest, BuildConfig.VERSION_NAME))
                .setPositiveButton(R.string.update_open,
                        (dialog, which) -> openReleasesPage(activity))
                .setNegativeButton(R.string.update_later, null)
                .setOnDismissListener(dialog -> prefs.edit()
                        .putString(LAST_NOTIFIED, latest).apply())
                .show();
    }

    private static String installerPackage(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                return context.getPackageManager().getInstallSourceInfo(
                        context.getPackageName()).getInstallingPackageName();
            }
            return context.getPackageManager().getInstallerPackageName(
                    context.getPackageName());
        } catch (Exception error) {
            return null;
        }
    }

    private static String fetchLatestTag() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent",
                    "MerossLocalPairer/" + BuildConfig.VERSION_NAME);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(),
                            StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                return new JSONObject(body.toString()).optString("tag_name", null);
            }
        } catch (Exception error) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
