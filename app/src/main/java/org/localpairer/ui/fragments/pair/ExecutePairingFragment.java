package org.localpairer.ui.fragments.pair;

import android.net.MacAddress;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import org.localpairer.AllowedWifiNetworks;
import org.localpairer.AndroidPreferencesManager;
import org.localpairer.Constants;
import org.localpairer.NetworkPolicy;
import org.localpairer.PairingLog;
import org.localpairer.PairingSession;
import org.localpairer.R;
import org.localpairer.model.MqttConfiguration;
import org.localpairer.model.exception.PermissionNotGrantedException;
import org.localpairer.ui.PairActivityViewModel;
import org.localpairer.ui.views.TaskLine;
import org.localpairer.merosslib.MerossDeviceAp;
import org.localpairer.merosslib.MerossHttpClient;
import org.localpairer.merosslib.model.OnlineStatus;
import org.localpairer.merosslib.model.http.ApiCredentials;
import org.localpairer.merosslib.model.http.DeviceInfo;
import org.localpairer.merosslib.model.http.exceptions.HttpApiException;

import java.io.IOException;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ExecutePairingFragment extends AbstractWifiFragment {
    private static final String TAG = "PairingFragment";
    private static final int WIFI_CONNECT_DELAY = 5000;

    private ScheduledExecutorService worker;

    private PairActivityViewModel pairActivityViewModel;
    private TextView errorDetailsTextView;
    private TaskLine connectWifiTaskLine, sendPairCommandTaskLine, connectLocalWifiTaskLine, checkOnlineTaskLine, currentTask;
    private Handler uiThreadHandler;

    private State state = State.INIT;
    private String error = null;

    private MerossDeviceAp mApDevice;
    private ApiCredentials mCreds;
    private String targetUuid;
    private volatile boolean destroyed;
    private Runnable pendingWifiConnectedCheck;
    private long pairingSessionId;

    public ExecutePairingFragment() {
        worker = Executors.newSingleThreadScheduledExecutor();
    }

    // Logic methods
    private void stateMachine(Signal signal) {
        if (!PairingSession.isCurrent(pairingSessionId)) {
            PairingLog.log("pairing", "ignoring " + signal + " from a stale session");
            return;
        }
        PairingLog.log("pairing", "state " + state + " -> " + signal);
        updateUi();

        if (signal == Signal.ERROR) {
            PairingLog.log("error", String.valueOf(error));
            state = State.ERROR;
            updateUi();
            return;
        }

        switch(state) {
            case INIT:
                if (signal == Signal.RESUMED) {connectToDeviceWifiAp();}
                break;
            case CONNECTING_DEVICE_WIFI_AP:
                if (signal == Signal.DEVICE_WIFI_CONNECTED) {configureDevice();}
                break;
            case SENDING_PAIRING_COMMAND:
                if (signal == Signal.DEVICE_CONFIGURED) {
                    // Skip connection polling if the device has been manually configured
                    ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(requireContext());
                    if (creds == null || creds.isManuallySet())
                        completeActivityFragment(false);
                    else
                        connectToLocalWifi();
                }
                break;
            case CONNETING_LOCAL_WIFI:
                if (signal == Signal.LOCAL_WIFI_CONNECTED) { pollDeviceList(); }
                break;
            case VERIFYING_PAIRING_SUCCEEDED:
                if (signal == Signal.DEVICE_PAIRED) {completeActivityFragment(true);}
                if (signal == Signal.MISSING_CONFIRMATION) {completeActivityFragment(false);}
                break;
        }

        updateUi();
    }

    @Override
    public void onDestroy() {
        destroyed = true;
        if (pendingWifiConnectedCheck != null) {
            uiThreadHandler.removeCallbacks(pendingWifiConnectedCheck);
            pendingWifiConnectedCheck = null;
        }
        if (worker != null) {
            // Stop any pending verification loop: it must never report success
            // for a later pairing attempt.
            worker.shutdownNow();
        }
        super.onDestroy();
        //requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void connectToDeviceWifiAp() {
        state = State.CONNECTING_DEVICE_WIFI_AP;

        String ssid = pairActivityViewModel.getMerossPairingAp().getValue().getSsid();
        String bssid = pairActivityViewModel.getMerossPairingAp().getValue().getBssid();
        PairingLog.log("wifi", "connecting to device AP ssid=" + ssid + " bssid=" + bssid);

        try {
            startWifiConnection(ssid, bssid,null, null, 15000);
            // Flow starts again from onWifiConnected() / onWifiUnavailable()
        } catch (PermissionNotGrantedException e) {
            Log.e(TAG, "Missing wifi permissions");
            // DO nothing, as the parent fragment will ask for permissions already.
            // Flow starts back from onWifiPermissionsGranted()
        }
    }

    private void connectToLocalWifi() {
        String ssid = pairActivityViewModel.getMerossConfiguredWifi().getValue().getScannedWifi().getSsid();
        String bssid = pairActivityViewModel.getMerossConfiguredWifi().getValue().getScannedWifi().getBssid();
        if (!AllowedWifiNetworks.isAllowed(requireContext(), ssid, bssid)) {
            NetworkPolicy.ensureAllowed(requireActivity(), ssid, bssid,
                    this::connectToLocalWifi);
            return;
        }
        state = State.CONNETING_LOCAL_WIFI;

        String passphrase = pairActivityViewModel.getMerossConfiguredWifi().getValue().getClearWifiPassword();
        PairingLog.log("wifi", "connecting to local wifi ssid=" + ssid + " bssid=" + bssid);

        // Check if we are already connected to such wifi
        // TODO: Check comparison happens with double quotes
        try {
            startWifiConnection(ssid, bssid, passphrase, null, 60000);
            // Flow starts again from onWifiConnected() / onWifiUnavailable()
        } catch (PermissionNotGrantedException e) {
            Log.e(TAG, "Missing wifi permissions");
            // Do nothing, as the parent fragment will ask for permissions already.
            // Flow starts back from onWifiPermissionsGranted()
        }
    }

    private void pollDeviceList() {
        state = State.VERIFYING_PAIRING_SUCCEEDED;

        org.localpairer.merosslib.model.protocol.MessageGetSystemAllResponse deviceInfo =
                pairActivityViewModel.getDeviceInfo().getValue();
        org.localpairer.model.MerossDeviceAp pairingAp = pairActivityViewModel.getMerossPairingAp().getValue();
        if (deviceInfo == null || pairingAp == null || mCreds == null) {
            error = "Pairing information is incomplete. Scan the device again.";
            stateMachine(Signal.ERROR);
            return;
        }

        final String requestedUuid = deviceInfo.getPayload().getAll().getSystem().getHardware().getUuid();
        final String requestedBssid = pairingAp.getBssid();
        final ApiCredentials requestedCreds = mCreds;
        targetUuid = requestedUuid;
        PairingLog.log("pairing", "verifying uuid=" + requestedUuid + " bssid=" + requestedBssid);

        final long timeout = GregorianCalendar.getInstance().getTimeInMillis() + Constants.PAIRING_VERIFY_TIMEOUT_MILLISECONDS;
        worker.schedule(new Runnable() {
            private @Nullable DeviceInfo findDevice(Collection<DeviceInfo> devices, String deviceUuid) {
                for (DeviceInfo d : devices) {
                    Log.d(TAG, "Device " + d.getUuid() + " has been found with status: " + d.getOnlineStatus());
                    if (d.getUuid().compareTo(deviceUuid) == 0) {
                        return d;
                    }
                }
                return null;
            }

            @Override
            public void run() {
                MerossHttpClient client = new MerossHttpClient(requestedCreds);
                boolean succeed = false;
                boolean timedOut = GregorianCalendar.getInstance().getTimeInMillis() >= timeout;
                boolean exitNow = false;
                while(!exitNow && !succeed && !Thread.currentThread().isInterrupted() && !timedOut) {
                    try {
                        List<DeviceInfo> devices = client.listDevices();
                        DeviceInfo d = findDevice(devices, requestedUuid);
                        if (d == null) {
                            Log.i(TAG, "Device " + requestedUuid + " not paired yet.");
                            PairingLog.log("verify", "uuid " + requestedUuid + " not in broker list");
                        } else if (d.getOnlineStatus() == OnlineStatus.ONLINE) {
                            Log.i(TAG, "Device " + requestedUuid + " is online.");
                            PairingLog.log("verify", "uuid " + requestedUuid + " ONLINE");
                            succeed = true;
                        } else {
                            Log.i(TAG, "Device " + requestedUuid + " is paired, but not ready yet or in an unknown status.");
                            PairingLog.log("verify", "uuid " + requestedUuid + " status " + d.getOnlineStatus());
                        }
                    } catch (HttpApiException e) {
                        e.printStackTrace();
                        Log.e(TAG, "The HTTP API server reported status " + e.getCode(), e);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "An IOException occurred while polling the HTTP API server.", e);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "An unexpected exception occurred", e);
                    } finally {
                        timedOut = GregorianCalendar.getInstance().getTimeInMillis() >= timeout;
                        error = "Timeout: waiting for confirmation from remote broker.";
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            exitNow = true;
                            error = "An error occurred while poling the HTTP API server";
                            stateMachine(Signal.ERROR);
                        }
                    }
                }

                final boolean finalSucceed = succeed;
                final boolean finalTimedOut = timedOut;
                uiThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (destroyed || !isAdded() || !PairingSession.isCurrent(pairingSessionId)) {
                            return;
                        }
                        org.localpairer.model.MerossDeviceAp currentAp = pairActivityViewModel.getMerossPairingAp().getValue();
                        boolean sameAttempt = currentAp != null && requestedBssid != null
                                && requestedBssid.equalsIgnoreCase(currentAp.getBssid());
                        if (!sameAttempt) {
                            Log.w(TAG, "Ignoring verification result from a previous pairing attempt.");
                            return;
                        }
                        if (finalSucceed)
                            stateMachine(Signal.DEVICE_PAIRED);
                        else if (finalTimedOut)
                            stateMachine(Signal.MISSING_CONFIRMATION);
                        else
                            stateMachine(Signal.ERROR);
                    }
                });
            }
        }, 2, TimeUnit.SECONDS);
    }

    private void configureDevice() {
        state = State.SENDING_PAIRING_COMMAND;
        if (mApDevice == null || mCreds == null
                || pairActivityViewModel.getTargetMqttConfig().getValue() == null
                || pairActivityViewModel.getMerossConfiguredWifi().getValue() == null) {
            error = "Pairing information is incomplete. Scan the device again.";
            stateMachine(Signal.ERROR);
            return;
        }
        // In custom credentials has been specified, override userId/key
        final String userId = pairActivityViewModel.getOverridedUserId().getValue() != null ? pairActivityViewModel.getOverridedUserId().getValue() : mCreds.getUserId();
        final String key = pairActivityViewModel.getOverridedKey().getValue() != null ? pairActivityViewModel.getOverridedKey().getValue() : mCreds.getKey();
        final MqttConfiguration targetConfig = pairActivityViewModel.getTargetMqttConfig().getValue();
        final org.localpairer.model.WifiConfiguration targetWifi = pairActivityViewModel.getMerossConfiguredWifi().getValue();

        worker.schedule(new Runnable() {
            @Override
            public void run() {
                if (!PairingSession.isCurrent(pairingSessionId)) {
                    PairingLog.log("pairing", "stale session, not configuring device");
                    return;
                }
                PairingLog.log("pairing", "setConfigKey broker=" + targetConfig.getHostname()
                        + ":" + targetConfig.getPort() + " targetUuid=" + targetUuid);
                try {
                    mApDevice.setConfigKey(
                            targetConfig.getHostname(),
                            targetConfig.getPort(),
                                    key,
                                    userId);
                    PairingLog.log("pairing", "setConfigKey acknowledged");
                } catch (IOException e) {
                    e.printStackTrace();
                    final String detail = e.getMessage();
                    uiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            error = "Error occurred while configuring device MQTT server: " + detail;
                            stateMachine(Signal.ERROR);
                        }
                    });
                    return;
                }

                // Give the device a moment to persist Config.Key before
                // switching it to the target WiFi.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    uiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            error = "Pairing was interrupted";
                            stateMachine(Signal.ERROR);
                        }
                    });
                    return;
                }

                if (!PairingSession.isCurrent(pairingSessionId)) {
                    PairingLog.log("pairing", "stale session, not configuring wifi");
                    return;
                }
                PairingLog.log("pairing", "setConfigWifi ssid="
                        + targetWifi.getScannedWifi().getSsid() + " targetUuid=" + targetUuid);
                try {
                    mApDevice.setConfigWifi(targetWifi.getScannedWifi(), targetWifi.getWifiPasswordBase64());
                    PairingLog.log("pairing", "setConfigWifi acknowledged");
                    stateMachine(Signal.DEVICE_CONFIGURED);
                } catch (IOException e) {
                    e.printStackTrace();
                    final String detail = e.getMessage();
                    uiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            error = "Error occurred while configuring device WIFI connection: " + detail;
                            stateMachine(Signal.ERROR);
                        }
                    });
                }

            }
        },1, TimeUnit.SECONDS);
    }

    private void completeActivityFragment(final boolean verified) {
        Runnable uiUpdater = new Runnable() {
            @Override
            public void run() {
                if (destroyed || !isAdded()) {
                    return;
                }
                PairingLog.log("pairing", "completed verified=" + verified
                        + " uuid=" + targetUuid);
                state = State.DONE;
                updateUi();
                NavController ctrl = NavHostFragment.findNavController(ExecutePairingFragment.this);
                Bundle args = new Bundle();
                args.putBoolean("verified", verified);
                args.putString("targetUuid", targetUuid);
                ctrl.navigate(R.id.action_executePair_to_pairCompleted, args, new NavOptions.Builder().setEnterAnim(android.R.animator.fade_in).setExitAnim(android.R.animator.fade_out).build());
            }
        };

        if (Looper.getMainLooper().getThread().getId() != Thread.currentThread().getId()) {
            uiThreadHandler.post(uiUpdater);
        } else {
            uiUpdater.run();
        }
    }

    // UI
    private void updateUi() {
        Runnable uiUpdater = new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case INIT:
                        errorDetailsTextView.setText("");
                        errorDetailsTextView.setVisibility(View.GONE);
                        connectWifiTaskLine.setState(TaskLine.TaskState.not_started);
                        sendPairCommandTaskLine.setState(TaskLine.TaskState.not_started);
                        connectLocalWifiTaskLine.setState(TaskLine.TaskState.not_started);
                        checkOnlineTaskLine.setState(TaskLine.TaskState.not_started);
                        currentTask = null;
                        break;
                    case CONNECTING_DEVICE_WIFI_AP:
                        errorDetailsTextView.setVisibility(View.GONE);
                        connectWifiTaskLine.setState(TaskLine.TaskState.running);
                        currentTask = connectWifiTaskLine;
                        break;
                    case SENDING_PAIRING_COMMAND:
                        errorDetailsTextView.setVisibility(View.GONE);
                        connectWifiTaskLine.setState(TaskLine.TaskState.completed);
                        sendPairCommandTaskLine.setState(TaskLine.TaskState.running);
                        currentTask = sendPairCommandTaskLine;
                        break;
                    case CONNETING_LOCAL_WIFI:
                        errorDetailsTextView.setVisibility(View.GONE);
                        sendPairCommandTaskLine.setState(TaskLine.TaskState.completed);
                        connectLocalWifiTaskLine.setState(TaskLine.TaskState.running);
                        currentTask = connectLocalWifiTaskLine;
                        break;
                    case VERIFYING_PAIRING_SUCCEEDED:
                        errorDetailsTextView.setVisibility(View.GONE);
                        connectLocalWifiTaskLine.setState(TaskLine.TaskState.completed);
                        checkOnlineTaskLine.setState(TaskLine.TaskState.running);
                        currentTask = checkOnlineTaskLine;
                        break;
                    case DONE:
                        if (connectLocalWifiTaskLine.getState() == TaskLine.TaskState.not_started)
                            connectLocalWifiTaskLine.setState(TaskLine.TaskState.skipped);
                        if (checkOnlineTaskLine.getState() == TaskLine.TaskState.not_started)
                            checkOnlineTaskLine.setState(TaskLine.TaskState.skipped);
                        errorDetailsTextView.setVisibility(View.GONE);
                        checkOnlineTaskLine.setState(TaskLine.TaskState.completed);
                        currentTask = null;
                        break;
                    case ERROR:
                        errorDetailsTextView.setText(error);
                        errorDetailsTextView.setVisibility(View.VISIBLE);
                        if (currentTask != null) {
                            currentTask.setState(TaskLine.TaskState.failed);
                        }
                        break;
                }
            }
        };

        if (Looper.getMainLooper().getThread().getId() != Thread.currentThread().getId()) {
            uiThreadHandler.post(uiUpdater);
        } else {
            uiUpdater.run();
        }
    }

    // Android activity lifecycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiThreadHandler = new Handler(Looper.getMainLooper());
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);
        pairingSessionId = pairActivityViewModel.getPairingSessionId();
    }

    @Override
    protected void onWifiConnected(final String ssid) {
        if (pendingWifiConnectedCheck != null) {
            uiThreadHandler.removeCallbacks(pendingWifiConnectedCheck);
        }
        pendingWifiConnectedCheck = new Runnable() {
            @Override
            public void run() {
                pendingWifiConnectedCheck = null;
                if (destroyed || !isAdded() || !PairingSession.isCurrent(pairingSessionId)) {
                    return;
                }
                PairingLog.log("wifi", "connected to " + ssid);
                if (pairActivityViewModel.getMerossConfiguredWifi().getValue() == null
                        || pairActivityViewModel.getMerossPairingAp().getValue() == null) {
                    error = "Pairing information is incomplete. Scan the device again.";
                    stateMachine(Signal.ERROR);
                    return;
                }
                String targetLocalSSID = pairActivityViewModel.getMerossConfiguredWifi().getValue().getScannedWifi().getSsid();
                String targetApSSID = pairActivityViewModel.getMerossPairingAp().getValue().getSsid();
                if (targetLocalSSID.compareTo(ssid) == 0) {
                    stateMachine(Signal.LOCAL_WIFI_CONNECTED);
                } else if (targetApSSID.compareTo(ssid) == 0) {
                    stateMachine(Signal.DEVICE_WIFI_CONNECTED);
                }
            }
        };
        uiThreadHandler.postDelayed(pendingWifiConnectedCheck, WIFI_CONNECT_DELAY);
    }

    @Override
    protected void onWifiUnavailable(String ssid) {
        error = "Failed to connect to " + ssid;
        stateMachine(Signal.ERROR);
    }

    @Override
    protected void onMissingWifiPermissions(String ssid) {
        error = "You must provide Wifi and Location access to this app to complete the operation.";
        stateMachine(Signal.ERROR);
    }

    @Override
    protected void onWifiPermissionsGranted(String ssid, @Nullable String bssid) {
        String pairingSsid = pairActivityViewModel.getMerossPairingAp().getValue().getSsid();
        String localSsid = pairActivityViewModel.getMerossConfiguredWifi().getValue().getScannedWifi().getSsid();

        if (ssid.compareTo(pairingSsid) == 0) {
            connectToDeviceWifiAp();
        } else if (ssid.compareTo(localSsid) == 0) {
            connectToLocalWifi();
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pair, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        connectWifiTaskLine = view.findViewById(R.id.connectApWifiTaskLine);
        sendPairCommandTaskLine = view.findViewById(R.id.sendPairCommandTaskLine);
        connectLocalWifiTaskLine = view.findViewById(R.id.connectToBrokerWifi);
        checkOnlineTaskLine = view.findViewById(R.id.connectToMqttBrokerTaskLike);
        errorDetailsTextView = view.findViewById(R.id.errorDetailTextView);

        //requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCreds = AndroidPreferencesManager.loadHttpCredentials(requireContext());
        mApDevice = pairActivityViewModel.getDevice().getValue();

        // As soon as we resume, connect to the given WiFi
        stateMachine(Signal.RESUMED);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    enum State {
        INIT,
        CONNECTING_DEVICE_WIFI_AP,
        SENDING_PAIRING_COMMAND,
        CONNETING_LOCAL_WIFI,
        VERIFYING_PAIRING_SUCCEEDED,
        DONE,
        ERROR
    }

    enum Signal {
        RESUMED,
        DEVICE_WIFI_CONNECTED,
        DEVICE_CONFIGURED,
        LOCAL_WIFI_CONNECTED,
        DEVICE_PAIRED,
        MISSING_CONFIRMATION,
        ERROR
    }
}
