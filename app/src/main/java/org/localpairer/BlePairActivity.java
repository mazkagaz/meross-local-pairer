package org.localpairer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONObject;
import org.localpairer.ble.MerossBleClient;
import org.localpairer.ble.MerossBleProtocol;
import org.localpairer.merosslib.MerossHttpClient;
import org.localpairer.merosslib.model.OnlineStatus;
import org.localpairer.merosslib.model.http.ApiCredentials;
import org.localpairer.merosslib.model.http.DeviceInfo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bluetooth provisioning path for devices that expose the Meross BLE service, such as recent MRS100 units. */
public class BlePairActivity extends AppCompatActivity implements MerossBleClient.Callback {
    public static final String EXTRA_ADDRESS = "ble_address";
    public static final String EXTRA_NAME = "ble_name";

    private static final int REQUEST_BLE_PERMISSIONS = 21;
    private static final int REQUEST_ENABLE_BT = 22;
    private static final int BROKER_PORT = 2001;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<BluetoothDevice> devices = new ArrayList<>();
    private final List<String> deviceLabels = new ArrayList<>();

    private MerossBleClient ble;
    private TextView statusView;
    private TextView deviceInfoView;
    private Spinner deviceSpinner;
    private Spinner wifiSpinner;
    private EditText passwordInput;
    private CheckBox rememberPasswordCheckBox;
    private EditText deviceNameInput;
    private Button pairButton;
    private ArrayAdapter<String> deviceAdapter;
    private ArrayAdapter<String> wifiAdapter;
    private List<MerossBleProtocol.WifiEntry> wifiEntries = new ArrayList<>();

    private String deviceType;
    private String deviceUuid;
    private String deviceMac;
    private boolean pairingSent;
    private String initialAddress;
    private String initialName;
    private final ExecutorService verifyExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialAddress = getIntent().getStringExtra(EXTRA_ADDRESS);
        initialName = getIntent().getStringExtra(EXTRA_NAME);
        setTitle("Bluetooth pairing");
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUi();
        ble = new MerossBleClient(this, this);
        PairingLog.log("ble", "BLE pairing screen opened");
        if (!ble.isSupported()) {
            setStatus("This phone does not support Bluetooth LE.");
            return;
        }
        requestPermissionsAndStart();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        statusView = new TextView(this);
        statusView.setTextSize(15);
        statusView.setText("Preparing Bluetooth...");
        root.addView(statusView);

        deviceInfoView = new TextView(this);
        deviceInfoView.setTextSize(14);
        deviceInfoView.setPadding(0, dp(12), 0, dp(12));
        root.addView(deviceInfoView);

        if (initialAddress == null) {
            root.addView(label("1. Meross BLE device"));
            deviceSpinner = new Spinner(this);
            deviceAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, deviceLabels);
            deviceSpinner.setAdapter(deviceAdapter);
            root.addView(deviceSpinner);

            Button scanButton = new Button(this);
            scanButton.setText("Rescan Bluetooth");
            scanButton.setOnClickListener(v -> startScan());
            root.addView(scanButton);

            Button connectButton = new Button(this);
            connectButton.setText("Connect to selected device");
            connectButton.setOnClickListener(v -> connectSelected());
            root.addView(connectButton);
        } else {
            setStatus("Bluetooth device " + (initialName != null ? initialName : initialAddress)
                    + " selected. Connecting...");
        }

        root.addView(label("2. Home Wi-Fi"));
        wifiSpinner = new Spinner(this);
        wifiAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        wifiSpinner.setAdapter(wifiAdapter);
        root.addView(wifiSpinner);

        passwordInput = new EditText(this);
        passwordInput.setHint("Wi-Fi password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(passwordInput);

        CheckBox showPasswordCheckBox = new CheckBox(this);
        showPasswordCheckBox.setText("Show password");
        showPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                passwordInput.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());
            } else {
                passwordInput.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());
            }
        });
        root.addView(showPasswordCheckBox);

        rememberPasswordCheckBox = new CheckBox(this);
        rememberPasswordCheckBox.setText("Remember this Wi-Fi password");
        rememberPasswordCheckBox.setChecked(true);
        root.addView(rememberPasswordCheckBox);

        wifiSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyStoredPassword();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        deviceNameInput = new EditText(this);
        deviceNameInput.setHint("Device name (required)");
        deviceNameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        root.addView(deviceNameInput);

        pairButton = new Button(this);
        pairButton.setText("Configure device");
        pairButton.setEnabled(false);
        pairButton.setOnClickListener(v -> configureDevice());
        root.addView(pairButton);

        setContentView(scroll);
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setGravity(Gravity.START);
        view.setPadding(0, dp(16), 0, dp(4));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void requestPermissionsAndStart() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= 37) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_LOCAL_NETWORK};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT};
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            if (!AndroidUtils.isLocationEnabled(this)) {
                setStatus("Enable location services: Android needs them for Bluetooth scanning.");
            }
        }
        boolean missing = false;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missing = true;
                break;
            }
        }
        if (missing) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_BLE_PERMISSIONS);
            return;
        }
        startBluetooth();
    }

    @SuppressLint("MissingPermission")
    private void startBluetooth() {
        // requestPermissionsAndStart() has already granted BLUETOOTH_CONNECT
        // before this method can request the system enable dialog.
        if (!ble.isEnabled()) {
            setStatus("Bluetooth is disabled. Enable it to continue.");
            Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable, REQUEST_ENABLE_BT);
            return;
        }
        if (initialAddress != null) {
            connectToAddress(initialAddress);
        } else {
            startScan();
        }
    }

    private void startScan() {
        devices.clear();
        deviceLabels.clear();
        deviceAdapter.notifyDataSetChanged();
        setStatus("Scanning for Meross BLE devices...");
        PairingLog.log("ble", "starting BLE scan");
        ble.startScan();
    }

    private void connectSelected() {
        int position = deviceSpinner.getSelectedItemPosition();
        if (position < 0 || position >= devices.size()) {
            Toast.makeText(this, "Select a device first.", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothDevice device = devices.get(position);
        connectToAddress(device.getAddress());
    }

    private void connectToAddress(String address) {
        setStatus("Connecting to " + address + "...");
        PairingLog.log("ble", "connecting to " + address);
        ble.connect(address);
    }

    private void configureDevice() {
        if (pairingSent) {
            return;
        }
        ApiCredentials credentials = AndroidPreferencesManager.loadHttpCredentials(this);
        if (credentials == null || isEmpty(credentials.getApiServer())
                || isEmpty(credentials.getUserId()) || isEmpty(credentials.getKey())) {
            setStatus("No local broker credentials. Configure the account first.");
            return;
        }
        String host = extractHost(credentials.getApiServer());
        if (host == null) {
            setStatus("Invalid broker address: " + credentials.getApiServer());
            return;
        }
        int position = wifiSpinner.getSelectedItemPosition();
        if (position < 0 || position >= wifiEntries.size()) {
            Toast.makeText(this, "Select a Wi-Fi network first.", Toast.LENGTH_SHORT).show();
            return;
        }
        MerossBleProtocol.WifiEntry wifi = wifiEntries.get(position);
        // The user explicitly chose this network as the device network, so
        // trust it even when the phone itself uses another Wi-Fi.
        AllowedWifiNetworks.add(this, wifi.ssid, wifi.bssid);
        AndroidPreferencesManager.clearWifiDeclined(this, wifi.ssid);
        AndroidPreferencesManager.storeLastDeviceWifi(
                this, wifi.ssid, wifi.bssid);
        String password = passwordInput.getText().toString();
        if (wifi.encryption != 0 && password.isEmpty()) {
            Toast.makeText(this, "Enter the Wi-Fi password.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (wifi.encryption != 0 && !password.isEmpty()
                && rememberPasswordCheckBox.isChecked()) {
            AndroidPreferencesManager.storeWifiStoredPassword(
                    this, wifi.ssid, wifi.bssid, password);
        }
        String deviceName = deviceNameInput.getText().toString().trim();
        if (deviceName.isEmpty()) {
            Toast.makeText(this, "Enter a device name.", Toast.LENGTH_SHORT).show();
            deviceNameInput.requestFocus();
            return;
        }
        if (deviceName.length() > 128) {
            Toast.makeText(this, "Device name is too long.", Toast.LENGTH_SHORT).show();
            deviceNameInput.requestFocus();
            return;
        }

        try {
            pairingSent = true;
            pairButton.setEnabled(false);
            setStatus("Sending broker configuration...");
            String encrypted = MerossBleProtocol.encryptWifiPassword(
                    password, deviceType, deviceUuid, deviceMac);

            JSONObject key = new JSONObject();
            key.put("userId", credentials.getUserId());
            key.put("key", credentials.getKey());
            JSONObject gateway = new JSONObject();
            gateway.put("host", host);
            gateway.put("port", BROKER_PORT);
            gateway.put("secondHost", host);
            gateway.put("secondPort", BROKER_PORT);
            key.put("gateway", gateway);
            JSONObject keyPayload = new JSONObject();
            keyPayload.put("key", key);
            PairingLog.log("ble", "SET Appliance.Config.Key broker=" + host + ":" + BROKER_PORT
                    + " userId=" + credentials.getUserId() + " uuid=" + deviceUuid);
            ble.send(MerossBleProtocol.packet(
                    "Appliance.Config.Key", "SET", keyPayload));

            JSONObject wifiPayload = new JSONObject();
            wifiPayload.put("wifi", wifi.toPayload(encrypted));
            handler.postDelayed(() -> {
                PairingLog.log("ble", "SET Appliance.Config.WifiX ssid=" + wifi.ssid);
                setStatus("Sending Wi-Fi configuration...");
                ble.send(MerossBleProtocol.packet(
                        "Appliance.Config.WifiX", "SET", wifiPayload));
                setStatus("Configuration sent. Waiting for the device to connect...");
                PairingLog.log("ble", "configuration sent for uuid " + deviceUuid);
                handler.postDelayed(() -> {
                    ble.disconnect();
                    verifyOnline(credentials, deviceUuid);
                }, 2500);
            }, 1200);
        } catch (Exception e) {
            PairingLog.log("ble", "configuration error: " + e.getMessage());
            setStatus("Configuration error: " + e.getMessage());
            pairingSent = false;
            pairButton.setEnabled(true);
        }
    }

    private void verifyOnline(ApiCredentials credentials, String uuid) {
        setStatus("Checking that the device is online in the broker...");
        final Handler main = new Handler(Looper.getMainLooper());
        final String mac = deviceMac;
        final String requestedName = deviceNameInput != null
                ? deviceNameInput.getText().toString().trim() : "";
        verifyExecutor.execute(() -> {
            MerossHttpClient client = new MerossHttpClient(credentials);
            boolean renamed = requestedName.isEmpty();
            // Re-pairing a forgotten device must lift its broker revocation
            // before the device can log in again.
            if (mac != null) {
                try {
                    client.claimDevice(mac);
                    PairingLog.log("ble", "claimed mac " + mac);
                } catch (Exception e) {
                    PairingLog.log("ble", "claim failed for mac " + mac + ": " + e.getMessage());
                }
            }
            long deadline = System.currentTimeMillis() + 60000;
            while (!destroyed && !Thread.currentThread().isInterrupted()
                    && System.currentTimeMillis() < deadline) {
                try {
                    List<DeviceInfo> list = client.listDevices();
                    for (DeviceInfo info : list) {
                        if (uuid == null || !uuid.equals(info.getUuid())) {
                            continue;
                        }
                        if (!renamed) {
                            renamed = true;
                            try {
                                client.renameDevice(uuid, requestedName);
                                PairingLog.log("ble", "renamed uuid " + uuid
                                        + " to " + requestedName);
                            } catch (Exception renameError) {
                                PairingLog.log("ble", "rename failed: "
                                        + renameError.getMessage());
                                main.post(() -> setStatus("Name not saved: "
                                        + renameError.getMessage()
                                        + "\nRename the device from the device list."));
                            }
                        }
                        if (info.getOnlineStatus() == OnlineStatus.ONLINE) {
                            PairingLog.log("ble", "device " + uuid + " is ONLINE");
                            main.post(() -> {
                                if (destroyed) {
                                    return;
                                }
                                setStatus("Device paired and ONLINE: " + uuid);
                                Toast.makeText(this, "Device paired and online.",
                                        Toast.LENGTH_LONG).show();
                                main.postDelayed(() -> {
                                    if (!destroyed) {
                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                }, 1500);
                            });
                            return;
                        }
                    }
                } catch (Exception e) {
                    PairingLog.log("ble", "verification error: " + e.getMessage());
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            main.post(() -> {
                if (!destroyed) {
                    setStatus("Configuration sent, but the device is not ONLINE yet: "
                            + uuid + "\nCheck the broker and the device Wi-Fi.");
                }
            });
        });
    }

    private void setStatus(String message) {
        statusView.setText(message);
        PairingLog.log("ble", message);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String extractHost(String apiServer) {
        if (isEmpty(apiServer)) {
            return null;
        }
        String value = apiServer.trim();
        try {
            URI uri = new URI(value.contains("://") ? value : "http://" + value);
            if (uri.getHost() != null && !uri.getHost().isEmpty()) {
                return uri.getHost();
            }
        } catch (URISyntaxException ignored) {
        }
        String host = value.replaceFirst("^[a-zA-Z]+://", "");
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        int colon = host.indexOf(':');
        if (colon >= 0) {
            host = host.substring(0, colon);
        }
        return host.isEmpty() ? null : host;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLE_PERMISSIONS) {
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                }
            }
            if (granted) {
                startBluetooth();
            } else {
                setStatus("Bluetooth permissions are required for Bluetooth pairing.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startBluetooth();
            } else {
                setStatus("Bluetooth must be enabled to pair this device.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        verifyExecutor.shutdownNow();
        if (ble != null) {
            ble.disconnect();
        }
        super.onDestroy();
    }

    // MerossBleClient callbacks

    @Override
    public void onDeviceFound(BluetoothDevice device, String name) {
        runOnUiThread(() -> {
            for (BluetoothDevice known : devices) {
                if (known.getAddress().equals(device.getAddress())) {
                    return;
                }
            }
            devices.add(device);
            deviceLabels.add((name != null ? name : "BLE device") + " (" + device.getAddress() + ")");
            deviceAdapter.notifyDataSetChanged();
            setStatus("Found " + devices.size() + " Meross BLE device(s). Select one and connect.");
            PairingLog.log("ble", "found " + name + " " + device.getAddress());
        });
    }

    @Override
    public void onReady() {
        runOnUiThread(() -> {
            setStatus("Connected. Reading device information...");
            deviceInfoView.setText("");
            ble.send(MerossBleProtocol.packet("Appliance.System.All", "GET", null));
        });
    }

    @Override
    public void onPacket(JSONObject packet) {
        runOnUiThread(() -> {
            try {
                String namespace = packet.getJSONObject("header").getString("namespace");
                JSONObject payload = packet.optJSONObject("payload");
                PairingLog.log("ble", "packet " + namespace + " " + payload);
                if ("Appliance.System.All".equals(namespace) && payload != null) {
                    handleSystemAll(payload);
                } else if ("Appliance.Config.WifiList".equals(namespace) && payload != null) {
                    handleWifiList(payload);
                }
            } catch (Exception e) {
                setStatus("Invalid BLE response: " + e.getMessage());
            }
        });
    }

    private void handleSystemAll(JSONObject payload) {
        try {
            JSONObject system = payload.getJSONObject("all").getJSONObject("system");
            JSONObject hardware = system.getJSONObject("hardware");
            JSONObject firmware = system.getJSONObject("firmware");
            deviceType = hardware.getString("type");
            deviceUuid = hardware.getString("uuid");
            deviceMac = hardware.getString("macAddress");
            String firmwareVersion = firmware.getString("version");
            deviceInfoView.setText("Device: " + deviceType
                    + "\nUUID: " + deviceUuid
                    + "\nMAC: " + deviceMac
                    + "\nFirmware: " + firmwareVersion);
            setStatus("Device identified. Reading its Wi-Fi list...");
            ble.send(MerossBleProtocol.packet(
                    "Appliance.Config.WifiList", "GET", null));
        } catch (Exception e) {
            setStatus("Could not parse device info: " + e.getMessage());
        }
    }

    private void handleWifiList(JSONObject payload) {
        try {
            wifiEntries = MerossBleProtocol.parseWifiList(payload);
            List<String> labels = new ArrayList<>();
            for (MerossBleProtocol.WifiEntry entry : wifiEntries) {
                labels.add(entry.ssid + " (" + entry.bssid + ")");
            }
            wifiAdapter.clear();
            wifiAdapter.addAll(labels);
            wifiAdapter.notifyDataSetChanged();
            pairButton.setEnabled(!wifiEntries.isEmpty());
            if (!wifiEntries.isEmpty()) {
                wifiSpinner.setSelection(preferredWifiIndex());
                applyStoredPassword();
            }
            setStatus(wifiEntries.isEmpty()
                    ? "The device did not report any Wi-Fi network."
                    : "Select the home Wi-Fi; a remembered password is filled automatically.");
        } catch (Exception e) {
            setStatus("Could not parse Wi-Fi list: " + e.getMessage());
        }
    }

    private int indexOfWifi(AllowedWifiNetworks.Entry target) {
        if (target == null || target.ssid == null) {
            return -1;
        }
        for (int i = 0; i < wifiEntries.size(); i++) {
            MerossBleProtocol.WifiEntry entry = wifiEntries.get(i);
            if (!target.ssid.equalsIgnoreCase(entry.ssid)) {
                continue;
            }
            if (target.bssid == null || target.bssid.trim().isEmpty()
                    || target.bssid.equalsIgnoreCase(entry.bssid)) {
                return i;
            }
        }
        return -1;
    }

    /** Prefers the last device network, then the phone's current network. */
    private int preferredWifiIndex() {
        AllowedWifiNetworks.Entry last =
                AndroidPreferencesManager.loadLastDeviceWifi(this);
        int lastIndex = indexOfWifi(last);
        if (lastIndex >= 0) {
            return lastIndex;
        }
        AllowedWifiNetworks.Entry current = AllowedWifiNetworks.current(this);
        if (current != null) {
            for (int i = 0; i < wifiEntries.size(); i++) {
                MerossBleProtocol.WifiEntry entry = wifiEntries.get(i);
                if (entry.ssid.equals(current.ssid)
                        && (current.bssid == null
                        || entry.bssid.equalsIgnoreCase(current.bssid))) {
                    return i;
                }
            }
        }
        // Networks the user already validated in the app count as remembered
        // even when their password was saved for another access point.
        List<AllowedWifiNetworks.Entry> allowed = AllowedWifiNetworks.load(this);
        for (AllowedWifiNetworks.Entry saved : allowed) {
            if (saved.ssid == null) {
                continue;
            }
            for (int i = 0; i < wifiEntries.size(); i++) {
                if (wifiEntries.get(i).ssid.equalsIgnoreCase(saved.ssid)) {
                    return i;
                }
            }
        }
        for (int i = 0; i < wifiEntries.size(); i++) {
            MerossBleProtocol.WifiEntry entry = wifiEntries.get(i);
            if (AndroidPreferencesManager.getWifiStoredPasswordForNetwork(
                    this, entry.ssid, entry.bssid) != null) {
                return i;
            }
        }
        return 0;
    }

    private void applyStoredPassword() {
        int position = wifiSpinner.getSelectedItemPosition();
        if (position < 0 || position >= wifiEntries.size()) {
            return;
        }
        MerossBleProtocol.WifiEntry entry = wifiEntries.get(position);
        String stored = AndroidPreferencesManager.getWifiStoredPasswordForNetwork(
                this, entry.ssid, entry.bssid);
        passwordInput.setText(stored != null ? stored : "");
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> setStatus("BLE error: " + message));
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> PairingLog.log("ble", "BLE disconnected"));
    }
}
