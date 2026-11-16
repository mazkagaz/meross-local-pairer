package org.localpairer.ui.fragments.pair;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CHANGE_NETWORK_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.Manifest.permission.NEARBY_WIFI_DEVICES;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.localpairer.AndroidUtils;
import org.localpairer.BlePairActivity;
import org.localpairer.MerossUtils;
import org.localpairer.PairingLog;
import org.localpairer.R;
import org.localpairer.ble.MerossBleClient;
import org.localpairer.model.MerossDeviceAp;
import org.localpairer.model.WifiLocationStatus;
import org.localpairer.ui.PairActivityViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ScanDevicesFragment extends Fragment {
    private PairActivityViewModel pairActivityViewModel;

    private WifiManager wifiManager = null;
    private ProgressBar scanningProgressBar;
    private FloatingActionButton fab;
    private MerossWifiScanAdapter adapter = new MerossWifiScanAdapter();
    private SwipeRefreshLayout swipeContainer;
    private TextView scanStatusTextView;
    private Handler uiHandler;
    private final SimpleDateFormat scanTimeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.US);
    private boolean scanning;
    private boolean followUpScansScheduled;
    private Runnable pendingScanRetry;
    private Runnable pendingAutoScan;
    private static final long AUTO_SCAN_INTERVAL_MS = 30000;
    private TextView bleSectionTitle;
    private LinearLayout bleDevicesContainer;
    private MerossBleClient bleScanner;
    private final List<String> bleAddresses = new ArrayList<>();
    private Runnable bleScanLoop;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;
    private static String[] REQUIRED_PERMISSIONS = null;
    private static final String STATE_N_TOKENS = "_state_num_tokens";
    private static final long DEFAULT_N_SCANS_PER_INTERVAL = 4;
    private static final long DEFAULT_INTERVAL_MINUTES = 2;
    private final TokenBucket scanBucket = TokenBuckets.builder().withCapacity(DEFAULT_N_SCANS_PER_INTERVAL).withFixedIntervalRefillStrategy(DEFAULT_N_SCANS_PER_INTERVAL, DEFAULT_INTERVAL_MINUTES, TimeUnit.MINUTES).build();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long tokens = DEFAULT_N_SCANS_PER_INTERVAL; // by default we are only allowed
        if (savedInstanceState != null)
            tokens = savedInstanceState.getLong(STATE_N_TOKENS);
        scanBucket.refill(tokens);
        if (android.os.Build.VERSION.SDK_INT > 32){
            // For releases > 32, we just need
            REQUIRED_PERMISSIONS = new String[] {
                    NEARBY_WIFI_DEVICES,
                    ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION,
                    CHANGE_WIFI_STATE,
                    CHANGE_NETWORK_STATE,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
            };
        } else{
            // For releases <= 32, we need to ask also for location permissions in order to scan
            // wifis
            REQUIRED_PERMISSIONS = new String[] {
                    ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION,
                    CHANGE_WIFI_STATE,
                    CHANGE_NETWORK_STATE };
        }

        this.wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.uiHandler = new Handler(Looper.getMainLooper());
        scanning = false;
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);

        multiplePermissionLauncher = registerForActivityResult(new ActivityResultContracts
                .RequestMultiplePermissions(), isGranted -> {
            Log.d("PERMISSIONS", "Launcher result: " + isGranted.toString());
            if (isGranted.containsValue(false)) {
                Log.d("PERMISSIONS", "At least one of the permissions was not granted, launching again...");
                Toast.makeText(requireContext(), "This app won't work without required permissions", Toast.LENGTH_LONG).show();
            } else {
                startScan();
                startBleScanning();
            }
        });
        multiplePermissionLauncher.launch(REQUIRED_PERMISSIONS);
    }

    private void askPermissions(ActivityResultLauncher<String[]> multiplePermissionLauncher) {
        if (!hasPermissions(REQUIRED_PERMISSIONS)) {
            Log.d("PERMISSIONS", "Launching multiple contract permission launcher for ALL required permissions");
            multiplePermissionLauncher.launch(REQUIRED_PERMISSIONS);
        } else {
            Log.d("PERMISSIONS", "All permissions are already granted");
        }
    }

    private boolean hasPermissions(String[] permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSIONS", "Permission is not granted: " + permission);
                    return false;
                }
                Log.d("PERMISSIONS", "Permission already granted: " + permission);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        cancelScanRetry();
        cancelFollowUpScans();
        stopBleScanning();
        if (bleScanner != null) {
            bleScanner.disconnect();
            bleScanner = null;
        }
        super.onDestroy();
    }

    @Override
    public void onStop() {
        // Never let a delayed scan fire while the user is connecting to the
        // device AP (this fragment stays alive on the back stack).
        cancelScanRetry();
        cancelFollowUpScans();
        stopBleScanning();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_N_TOKENS, scanBucket.getNumTokens());
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        requireContext().registerReceiver(wifiScanReceiver, intentFilter);

        checkWifiAndLocation();
        startBleScanning();
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(wifiScanReceiver);
    }

    private boolean hasBlePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return hasPermissions(new String[]{
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT});
        }
        return true;
    }

    private void startBleScanning() {
        if (bleScanner == null || !isAdded() || !isResumed() || !hasBlePermissions()) {
            return;
        }
        stopBleScanning();
        bleScanLoop = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || !isResumed()) {
                    return;
                }
                bleScanner.startScan();
                bleScanLoop = this;
                uiHandler.postDelayed(this, 13000);
            }
        };
        uiHandler.post(bleScanLoop);
    }

    private void stopBleScanning() {
        if (bleScanLoop != null) {
            uiHandler.removeCallbacks(bleScanLoop);
            bleScanLoop = null;
        }
        if (bleScanner != null) {
            bleScanner.stopScan();
        }
    }

    private void addBleDevice(BluetoothDevice device, String name) {
        if (bleDevicesContainer == null || bleAddresses.contains(device.getAddress())) {
            return;
        }
        bleAddresses.add(device.getAddress());
        String label = (name != null ? name : "Meross Bluetooth") + "\n" + device.getAddress();
        TextView row = new TextView(requireContext());
        row.setText(label);
        row.setTextSize(14);
        row.setPadding(8, 12, 8, 12);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BlePairActivity.class);
            intent.putExtra(BlePairActivity.EXTRA_ADDRESS, device.getAddress());
            intent.putExtra(BlePairActivity.EXTRA_NAME, name);
            startActivity(intent);
        });
        bleDevicesContainer.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        bleDevicesContainer.setVisibility(View.VISIBLE);
        bleSectionTitle.setVisibility(View.VISIBLE);
        PairingLog.log("ble", "found " + name + " " + device.getAddress());
    }

    private final MerossBleClient.Callback bleScanCallback = new MerossBleClient.Callback() {
        @Override
        public void onDeviceFound(BluetoothDevice device, String name) {
            uiHandler.post(() -> addBleDevice(device, name));
        }

        @Override
        public void onReady() {
        }

        @Override
        public void onPacket(org.json.JSONObject packet) {
        }

        @Override
        public void onError(String message) {
            PairingLog.log("ble", "scan error: " + message);
        }

        @Override
        public void onDisconnected() {
        }
    };

    private boolean checkWifiAndLocation() {
        // Show an error message if wifi is not enabled
        if (!AndroidUtils.isWifiEnabled(requireContext())) {
            Snackbar.make(getView(), "Please enable Wifi network to perform the scan", Snackbar.LENGTH_LONG).show();
            return false;
        }

        if (!AndroidUtils.isLocationEnabled(getContext())) {
            Snackbar.make(getView(), "Please enable Location services to perform the scan", Snackbar.LENGTH_LONG).show();
            return false;
        }

        return true;
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeContainer = view.findViewById(R.id.swipeContainer);
        scanStatusTextView = view.findViewById(R.id.scanStatusTextView);
        bleSectionTitle = view.findViewById(R.id.bleSectionTitle);
        bleDevicesContainer = view.findViewById(R.id.bleDevicesContainer);
        boolean bleSupported = requireContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        if (bleSupported) {
            bleScanner = new MerossBleClient(requireContext(), bleScanCallback);
        } else {
            bleSectionTitle.setVisibility(View.GONE);
            bleDevicesContainer.setVisibility(View.GONE);
        }
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                swipeContainer.setRefreshing(false);
                startScan();
            }
        });

        scanningProgressBar = view.findViewById(R.id.scanningProgressSpinner);
        final RecyclerView recyclerView = view.findViewById(R.id.wifiList);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(adapter);

        fab = view.findViewById(R.id.scan_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });

        final TextView enableWifiAndLocationTextView = view.findViewById(R.id.enableWifiAndLocationTextView);
        pairActivityViewModel.getWifiLocationStatus().observe(requireActivity(), new Observer<WifiLocationStatus>() {
            @Override
            public void onChanged(WifiLocationStatus wifiLocationStatus) {
                boolean wifiOk = wifiLocationStatus.getWifiEnabledOrEnabling()!=null && wifiLocationStatus.getWifiEnabledOrEnabling();
                boolean locationOk = wifiLocationStatus.getLocationEnabledOrEnabling()!=null && wifiLocationStatus.getLocationEnabledOrEnabling();
                enableWifiAndLocationTextView.setVisibility(wifiOk && locationOk ? View.GONE:View.VISIBLE);
                recyclerView.setVisibility(wifiOk && locationOk ? View.VISIBLE : View.GONE);
                fab.setEnabled(wifiOk && locationOk);
            }
        });
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                scanSuccess();
            } else {
                // scan failure handling
                scanFailure();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        startScan(false);
        scheduleFollowUpScans();
    }

    private void scheduleFollowUpScans() {
        if (followUpScansScheduled || adapter.getItemCount() > 0) {
            return;
        }
        // Some devices bring their AP up late, or keep it for a short time.
        // Keep a slow automatic scan running while the screen is visible and
        // no device was found; 30 s stays within Android's scan budget.
        followUpScansScheduled = true;
        pendingAutoScan = new Runnable() {
            @Override
            public void run() {
                pendingAutoScan = null;
                if (!isAdded() || !isResumed()) {
                    return;
                }
                if (adapter.getItemCount() > 0) {
                    return;
                }
                if (!scanning) {
                    startScan(false);
                }
                pendingAutoScan = this;
                uiHandler.postDelayed(this, AUTO_SCAN_INTERVAL_MS);
            }
        };
        uiHandler.postDelayed(pendingAutoScan, AUTO_SCAN_INTERVAL_MS);
    }

    private void cancelFollowUpScans() {
        if (pendingAutoScan != null) {
            uiHandler.removeCallbacks(pendingAutoScan);
            pendingAutoScan = null;
        }
        followUpScansScheduled = false;
    }

    private void showScanMessage(String message, int length) {
        PairingLog.log("scan", message);
        if (isAdded() && getView() != null) {
            Snackbar.make(getView(), message, length).show();
        }
    }

    private void updateScanStatus(String message) {
        if (scanStatusTextView != null) {
            scanStatusTextView.setText(message);
        }
    }

    private void startScan() {
        startScan(true);
    }

    private void startScan(boolean userInitiated) {
        if (!scanBucket.tryConsume(1)) {
            if (userInitiated) {
                long seconds = scanBucket.getDurationUntilNextRefill(TimeUnit.SECONDS);
                showScanMessage("Scan limit reached; retry in " + seconds + " s.", Snackbar.LENGTH_LONG);
                updateScanStatus("Android scan limit reached. Automatic search resumes in "
                        + seconds + " s.");
            }
            return ;
        }

        if (scanning) {
            if (userInitiated) {
                showScanMessage("A scan is already running. Results will refresh shortly.",
                        Snackbar.LENGTH_SHORT);
            }
            return;
        }

        if (!checkWifiAndLocation()) {
            return;
        }

        if (userInitiated) {
            showScanMessage("Scanning for Meross devices in pairing mode...", Snackbar.LENGTH_SHORT);
        }

        if (!AndroidUtils.checkPermissions(requireContext(), REQUIRED_PERMISSIONS)) {
            AlertDialog.Builder permissionAlert = new AlertDialog.Builder(requireContext());
            permissionAlert.setTitle("Permission requests");
            permissionAlert.setMessage("Wifi scanning requires access to geolocation services (this is a requirement on Android).");
            permissionAlert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    askPermissions(multiplePermissionLauncher);
                }
            });
            permissionAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getContext(), "Aborted Wifi scan: permissions denied.", Toast.LENGTH_SHORT).show();
                }
            });
            permissionAlert.show();

        } else {
            fab.hide();
            scanning = true;
            scanningProgressBar.setVisibility(View.VISIBLE);
            updateScanStatus("Scan in progress...");
            boolean success = wifiManager.startScan();
            // Setup a timer
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    scanningProgressBar.setVisibility(View.GONE);
                    fab.show();
                }
            }, 10000);

            if (!success) {
                scanFailure();
            }
        }
    }

    private void updateScanData(List<ScanResult> wifiNetworks) {
        // Keep the raw scan visible in diagnostics: an AP with an unexpected
        // SSID must not silently disappear behind the Meross filter.
        StringBuilder raw = new StringBuilder();
        int rawLimit = Math.min(wifiNetworks.size(), 30);
        for (int i = 0; i < rawLimit; i++) {
            ScanResult r = wifiNetworks.get(i);
            if (raw.length() > 0) raw.append(", ");
            raw.append(r.SSID).append('/').append(r.BSSID);
        }
        if (wifiNetworks.size() > rawLimit) raw.append(", ...");
        PairingLog.log("scan", "raw scan " + wifiNetworks.size() + " AP(s): " + raw);

        // Only filter access points that match the Meross
        ArrayList<ScanResult> data = new ArrayList<>();
        for (ScanResult r : wifiNetworks) {
            if (MerossUtils.isMerossAp(r.SSID))
                data.add(r);
        }

        adapter.updateData(data);
        if (!data.isEmpty()) {
            // A visible device is enough: keep the remaining scan budget for
            // the manual rescan button.
            cancelFollowUpScans();
        }
        fab.show();
        scanningProgressBar.setVisibility(View.GONE);
        this.scanning = false;

        StringBuilder found = new StringBuilder();
        for (ScanResult r : data) {
            if (found.length() > 0) found.append(", ");
            found.append(r.SSID).append('/').append(r.BSSID);
        }
        PairingLog.log("scan", "found " + data.size() + " Meross AP(s): " + found);

        String scanTime;
        synchronized (scanTimeFormat) {
            scanTime = scanTimeFormat.format(new Date());
        }
        if (data.isEmpty()) {
            updateScanStatus("Last scan at " + scanTime
                    + ": 0 Meross device. Automatic search every 30 s.");
        } else {
            updateScanStatus("Last scan at " + scanTime + ": " + data.size()
                    + " Meross device(s). Tap one to pair it.");
        }

        if (data.size() > 1 && isAdded() && getView() != null) {
            Snackbar.make(getView(),
                    "Several Meross devices are in pairing mode. Check the SSID and BSSID before choosing.",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @SuppressLint("MissingPermission") // Scan is only triggered upon permissions checks
    private void scanSuccess() {
        cancelScanRetry();
        updateScanData(wifiManager.getScanResults());
    }

    @SuppressLint("MissingPermission") // Scan is only triggered upon permissions checks
    private void scanFailure() {
        // Android can refuse a scan (rate limiting, Wi-Fi busy). Keep the last
        // results and retry shortly instead of treating it as "no device".
        updateScanData(wifiManager.getScanResults());
        showScanMessage("Wi-Fi scan busy; retrying in 3 s...", Snackbar.LENGTH_SHORT);
        scheduleScanRetry();
    }

    private void scheduleScanRetry() {
        cancelScanRetry();
        pendingScanRetry = () -> {
            pendingScanRetry = null;
            if (isAdded() && isResumed() && !scanning) {
                startScan(false);
            }
        };
        uiHandler.postDelayed(pendingScanRetry, 3000);
    }

    private void cancelScanRetry() {
        if (pendingScanRetry != null) {
            uiHandler.removeCallbacks(pendingScanRetry);
            pendingScanRetry = null;
        }
    }

    class MerossWifiScanAdapter extends RecyclerView.Adapter<MerossWifiScanAdapter.MyViewHolder>{
        private ArrayList<ScanResult> scanResult;

        MerossWifiScanAdapter(){
            this.scanResult = new ArrayList<>();
        }

        public void updateData(List<ScanResult> data) {
            this.scanResult.clear();
            this.scanResult.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.meross_wifi_scan_result, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
            final ScanResult sr = this.scanResult.get(position);
            holder.updateScanResult(sr);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Drop any state left by a previous attempt before starting
                    // a new pairing session.
                    pairActivityViewModel.resetPairingState();
                    MerossDeviceAp targetAp = new MerossDeviceAp(sr.SSID,sr.BSSID);
                    pairActivityViewModel.setMerossPairingAp(targetAp);
                    PairingLog.log("scan", "selected SSID=" + sr.SSID + " BSSID=" + sr.BSSID
                            + " RSSI=" + sr.level);
                    NavHostFragment
                            .findNavController(ScanDevicesFragment.this)
                            .navigate(R.id.action_scan_to_fetchDeviceInfo,null, new NavOptions.Builder().setEnterAnim(android.R.animator.fade_in).setExitAnim(android.R.animator.fade_out).build());
                }
            });
        }

        @Override
        public int getItemCount() {
            return scanResult.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder{
            private final TextView wifiName;
            private final TextView bssidName;
            private final ImageView signalStrength;
            private ScanResult scanResult;

            MyViewHolder(View itemView) {
                super(itemView);
                wifiName = (TextView) itemView.findViewById(R.id.ssid);
                bssidName = (TextView) itemView.findViewById(R.id.bssid);
                signalStrength = (ImageView)itemView.findViewById(R.id.wifiSignalStrength);
            }

            public void updateScanResult(ScanResult sr) {
                this.scanResult = sr;
                this.wifiName.setText(sr.SSID);
                this.bssidName.setText(sr.BSSID);
                int signalLevel = sr.level;
                if (signalLevel>=-40)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_24dp);
                else if (signalLevel>=-50)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_24dp);
                else if (signalLevel>=-60)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_24dp);
                else if (signalLevel>=-70)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_24dp);
                else
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_24dp);
            }
        }
    }
}
