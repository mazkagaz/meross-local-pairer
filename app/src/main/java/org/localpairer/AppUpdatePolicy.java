package org.localpairer;

import java.util.regex.Pattern;

/** Pure update-check rules, testable without Android. */
public final class AppUpdatePolicy {

    private static final Pattern NUMERIC_VERSION =
            Pattern.compile("\\d+(\\.\\d+)*");

    private AppUpdatePolicy() {
    }

    public static boolean isFdroidInstaller(String installerPackage) {
        return "org.fdroid.fdroid".equals(installerPackage)
                || "org.fdroid.basic".equals(installerPackage);
    }

    public static boolean updatesDisabled(boolean fdroidBuild,
                                          String installerPackage) {
        return fdroidBuild || isFdroidInstaller(installerPackage);
    }

    public static String normalizeTag(String tag) {
        if (tag == null) {
            return null;
        }
        String normalized = tag.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    public static boolean isNewer(String currentVersion, String latestTag) {
        int[] current = parse(currentVersion);
        int[] latest = parse(normalizeTag(latestTag));
        if (current == null || latest == null) {
            return false;
        }
        int length = Math.max(current.length, latest.length);
        for (int index = 0; index < length; index++) {
            int currentPart = index < current.length ? current[index] : 0;
            int latestPart = index < latest.length ? latest[index] : 0;
            if (latestPart != currentPart) {
                return latestPart > currentPart;
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        if (version == null || !NUMERIC_VERSION.matcher(version).matches()) {
            return null;
        }
        String[] parts = version.split("\\.");
        int[] numbers = new int[parts.length];
        try {
            for (int index = 0; index < parts.length; index++) {
                numbers[index] = Integer.parseInt(parts[index]);
            }
        } catch (NumberFormatException error) {
            return null;
        }
        return numbers;
    }
}
