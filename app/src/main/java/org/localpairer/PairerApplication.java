package org.localpairer;

import android.app.Application;

/** Installs process-wide settings before any activity starts. */
public class PairerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidTlsPolicy.install(this);
    }
}
