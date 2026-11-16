package org.localpairer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.localpairer.ui.MainActivityViewModel;

import static android.location.LocationManager.PROVIDERS_CHANGED_ACTION;
import static android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION;

public class MainActivity extends AppCompatActivity {

    private BroadcastReceiver mReceiver;
    private TextView wifiTextView;
    private TextView wifiMerossWarning;
    private TextView locationTextView;
    private LinearLayout wifiLocationStatusLayout;
    private boolean mActiveFragmentsRequireWifiLocationWarn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        PairingLog.setEnabled(AndroidPreferencesManager.isPairingDebugEnabled(this));
        hideSystemUI();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        // Only the device list is a top-level destination; secondary screens
        // get the standard toolbar back arrow instead of an in-page button.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.devices_fragment).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Share the stored credentials with the fragments (Account in particular).
        MainActivityViewModel mainActivityViewModel =
                new ViewModelProvider(this).get(MainActivityViewModel.class);
        mainActivityViewModel.setCredentials(AndroidPreferencesManager.loadHttpCredentials(this));

        // Open the device list by default once an API is configured. Without
        // credentials, the account/login destination stays visible.
        if (savedInstanceState == null && AndroidPreferencesManager.loadHttpCredentials(this) != null) {
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(navController.getGraph().getStartDestination(), true)
                    .build();
            navController.navigate(R.id.devices_fragment, null, options);
        }

        wifiTextView = findViewById(R.id.wifiOffTextView);
        wifiMerossWarning = findViewById(R.id.wifiMerossWarning);
        locationTextView = findViewById(R.id.locationOffTextView);
        wifiLocationStatusLayout = findViewById(R.id.wifiLocationStatusLayout);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(NETWORK_STATE_CHANGED_ACTION) || intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    String connectedSsid = null;
                    connectedSsid = AndroidUtils.getConnectedWifi(MainActivity.this);
                    updateWifiStatus(connectedSsid!=null, connectedSsid);
                } else if (intent.getAction().equals(PROVIDERS_CHANGED_ACTION)) {
                    Boolean locationEnabled = null;
                    updateLocationStatus(AndroidUtils.isLocationEnabled(MainActivity.this));
                }
            }
        };
    }

    private void updateWifiStatus(boolean wifiEnabled, @Nullable String connctedSsid) {
        wifiTextView.setVisibility(wifiEnabled ? View.GONE : View.VISIBLE);
        wifiMerossWarning.setVisibility(MerossUtils.isMerossAp(connctedSsid) ? View.VISIBLE : View.GONE);
        updateStatusBarVisibility();
    }

    private void updateLocationStatus(boolean locationEnabled) {
        locationTextView.setVisibility(locationEnabled ? View.GONE : View.VISIBLE);
        updateStatusBarVisibility();
    }

    public void setWifiLocationWarnRequired(boolean enabled) {
        mActiveFragmentsRequireWifiLocationWarn = enabled;
        updateStatusBarVisibility();
    }

    public void updateStatusBarVisibility() {
        if (mActiveFragmentsRequireWifiLocationWarn) {
            wifiLocationStatusLayout.setVisibility(
                    wifiTextView.getVisibility() == View.VISIBLE ||
                            locationTextView.getVisibility() == View.VISIBLE ||
                            wifiMerossWarning.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        } else {
            wifiLocationStatusLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(PROVIDERS_CHANGED_ACTION);
        this.registerReceiver(this.mReceiver, filter);

        // Update wifi/location status
        String connectedWifi = AndroidUtils.getConnectedWifi(MainActivity.this);
        boolean locationEnabled = AndroidUtils.isLocationEnabled(MainActivity.this);
        updateWifiStatus(connectedWifi!=null, connectedWifi);
        updateLocationStatus(locationEnabled);

        WifiTrustPrompt.maybePrompt(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        WifiTrustPrompt.onPermissionsResult(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(this.mReceiver);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }


    private static final int TIME_INTERVAL = 2000; // # milliseconds, desired time passed between two back presses.
    private long mBackPressed;
    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        int backFrags = fragmentManager.findFragmentById(R.id.nav_host_fragment).getChildFragmentManager().getBackStackEntryCount();

        // If there are more fragments, popback (default behaviour)
        if (backFrags>0)
            super.onBackPressed();

        // otherwise ask for confirmation to exit
        else if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            Toast.makeText(getBaseContext(), "Tap back button in order to exit", Toast.LENGTH_SHORT).show();
        }

        mBackPressed = System.currentTimeMillis();
    }

}
