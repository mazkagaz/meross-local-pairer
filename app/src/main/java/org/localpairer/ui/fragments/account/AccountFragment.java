package org.localpairer.ui.fragments.account;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import org.localpairer.AllowedWifiNetworks;
import org.localpairer.AndroidPreferencesManager;
import org.localpairer.AndroidTlsPolicy;
import org.localpairer.AndroidUtils;
import org.localpairer.BrokerEndpoint;
import org.localpairer.Constants;
import org.localpairer.MainActivity;
import org.localpairer.MerossUtils;
import org.localpairer.PairingLog;
import org.localpairer.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import org.localpairer.model.HttpClientManager;
import org.localpairer.ui.MainActivityViewModel;
import org.localpairer.ui.fragments.login.LoginFragment;
import com.google.android.material.card.MaterialCardView;
import org.localpairer.merosslib.model.http.ApiCredentials;



import java.util.Locale;
import java.util.List;

public class AccountFragment extends Fragment {
    private static final int REQUEST_WIFI_PERMISSIONS = 41;
    private Runnable pendingWifiAction;
    private boolean accountSectionExpanded;
    private TextView tlsPinSummaryView;
    private TextView allowedWifiSummaryView;
    private final AllowedWifiNetworks.Listener allowedWifiListener = () -> {
        if (allowedWifiSummaryView != null && isAdded()) {
            refreshAllowedWifiSummary(allowedWifiSummaryView);
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final MainActivityViewModel mainActivityViewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);
        View root = inflater.inflate(R.layout.fragment_account, container, false);
        final MaterialCardView httpInfoCard = root.findViewById(R.id.httpInfoCard);
        final EditText brokerHostEditText = root.findViewById(R.id.brokerHostEditText);
        final SwitchMaterial httpsSwitch = root.findViewById(R.id.httpsSwitch);
        final EditText httpPortEditText = root.findViewById(R.id.httpPortEditText);
        final EditText httpsPortEditText = root.findViewById(R.id.httpsPortEditText);
        final TextView endpointSummary = root.findViewById(R.id.endpointSummary);
        final Button saveBrokerButton = root.findViewById(R.id.saveBrokerButton);
        final Button reloginButton = root.findViewById(R.id.reloginButton);
        final SwitchMaterial advancedModeSwitch = root.findViewById(R.id.advancedModeSwitch);
        final EditText userIdEditText = root.findViewById(R.id.userIdEditText);
        final EditText httpTokenEditText = root.findViewById(R.id.httpTokenEditText);
        final EditText mqttKeyEditText = root.findViewById(R.id.mqttKeyEditText);
        final Button httpLogoutButton = root.findViewById(R.id.httpLogoutButton);
        final Button haBrokerLoginButton = root.findViewById(R.id.haBrokerLoginButton);
        final MaterialCardView loginCardView = root.findViewById(R.id.loginCard);
        final Button manualSetupButton = root.findViewById(R.id.setManualButton);
        final Button manageAllowedWifiButton =
                root.findViewById(R.id.manageAllowedWifiButton);

        final BrokerEndpoint[] currentEndpoint = {
                AndroidPreferencesManager.loadBrokerEndpoint(requireContext())
        };
        brokerHostEditText.setText(currentEndpoint[0].getHost());
        httpsSwitch.setChecked(currentEndpoint[0].isHttps());
        httpPortEditText.setText(String.valueOf(currentEndpoint[0].getHttpPort()));
        httpsPortEditText.setText(String.valueOf(currentEndpoint[0].getHttpsPort()));
        updateEndpointSummary(endpointSummary, brokerHostEditText, httpsSwitch,
                httpPortEditText, httpsPortEditText);
        advancedModeSwitch.setChecked(AndroidPreferencesManager.isAdvancedMode(requireContext()));
        applyAdvancedVisibility(root);

        // Trusted Wi-Fi must be validated before any broker/API login. Put the
        // Wi-Fi section first so this is the first thing configured.
        View wifiHeader = root.findViewById(R.id.wifiSectionHeader);
        View wifiCard = root.findViewById(R.id.allowedWifiCard);
        View accountHeader = root.findViewById(R.id.accountSectionHeader);
        if (wifiHeader != null && wifiCard != null && accountHeader != null
                && accountHeader.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) accountHeader.getParent();
            int target = parent.indexOfChild(accountHeader);
            parent.removeView(wifiHeader);
            parent.removeView(wifiCard);
            parent.addView(wifiHeader, target);
            parent.addView(wifiCard, target + 1);
        }

        boolean firstConfig = AndroidPreferencesManager.loadHttpCredentials(requireContext()) == null;
        accountSectionExpanded = firstConfig;
        setAccountSection(root);
        root.findViewById(R.id.accountSectionHeader).setOnClickListener(v -> {
            accountSectionExpanded = !accountSectionExpanded;
            setAccountSection(root);
        });
        setupCardSection(root, R.id.wifiSectionHeader, R.id.wifiSectionArrow,
                R.id.allowedWifiCard, firstConfig);
        setupCardSection(root, R.id.diagnosticsSectionHeader, R.id.diagnosticsSectionArrow,
                R.id.diagnosticsCard, false);

        mainActivityViewModel.getCredentials().observe(getViewLifecycleOwner(), new Observer<ApiCredentials>() {
            @Override
            public void onChanged(ApiCredentials apiCredentials) {
                if (apiCredentials == null || TextUtils.isEmpty(apiCredentials.getUserId())) {
                    userIdEditText.setText(R.string.text_not_set);
                } else {
                    userIdEditText.setText(apiCredentials.getUserId());
                }

                if (apiCredentials == null || TextUtils.isEmpty(apiCredentials.getToken())) {
                    httpTokenEditText.setText(R.string.text_not_set);
                } else {
                    httpTokenEditText.setText(apiCredentials.getToken());
                }

                if (apiCredentials == null || TextUtils.isEmpty(apiCredentials.getKey())) {
                    mqttKeyEditText.setText(R.string.text_not_set_empty);
                } else {
                    mqttKeyEditText.setText(apiCredentials.getKey());
                }

                if (apiCredentials != null && apiCredentials.isManuallySet()) {
                    httpLogoutButton.setText(R.string.action_discard);
                } else {
                    httpLogoutButton.setText(R.string.action_logout);
                }

                httpLogoutButton.setEnabled(apiCredentials != null);
                httpInfoCard.setVisibility(accountSectionExpanded && apiCredentials != null
                        ? View.VISIBLE : View.GONE);
                loginCardView.setVisibility(accountSectionExpanded && apiCredentials == null
                        ? View.VISIBLE : View.GONE);
                haBrokerLoginButton.setEnabled(apiCredentials == null);
                boolean advanced = AndroidPreferencesManager.isAdvancedMode(requireContext());
                manualSetupButton.setVisibility(apiCredentials == null && advanced
                        ? View.VISIBLE : View.GONE);
                reloginButton.setVisibility(apiCredentials != null
                        ? View.VISIBLE : View.GONE);
            }
        });

        TextWatcher endpointWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateEndpointSummary(endpointSummary, brokerHostEditText, httpsSwitch,
                        httpPortEditText, httpsPortEditText);
            }
        };
        brokerHostEditText.addTextChangedListener(endpointWatcher);
        httpPortEditText.addTextChangedListener(endpointWatcher);
        httpsPortEditText.addTextChangedListener(endpointWatcher);
        httpsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateEndpointSummary(endpointSummary, brokerHostEditText, httpsSwitch,
                        httpPortEditText, httpsPortEditText));

        advancedModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AndroidPreferencesManager.setAdvancedMode(requireContext(), isChecked);
            applyAdvancedVisibility(root);
            refreshTlsPinSummary();
        });

        saveBrokerButton.setOnClickListener(v -> {
            BrokerEndpoint endpoint = readEndpoint(brokerHostEditText, httpsSwitch,
                    httpPortEditText, httpsPortEditText);
            if (!endpoint.isValid()) {
                brokerHostEditText.setError("Please input the Home Assistant address");
                return;
            }
            if (endpoint.requiresHttps()) {
                brokerHostEditText.setError("HTTPS is required for the broker API");
                return;
            }
            int selectedPort = endpoint.isHttps()
                    ? endpoint.getHttpsPort() : endpoint.getHttpPort();
            if (selectedPort == 2003) {
                (endpoint.isHttps() ? httpsPortEditText : httpPortEditText)
                        .setError("Port 2003 is reserved.");
                return;
            }
            httpPortEditText.setError(null);
            httpsPortEditText.setError(null);
            brokerHostEditText.setError(null);
            boolean hostChanged =
                    !endpoint.getHost().equalsIgnoreCase(currentEndpoint[0].getHost());
            AndroidPreferencesManager.saveBrokerEndpoint(requireContext(), endpoint);
            currentEndpoint[0] = endpoint;

            ApiCredentials credentials =
                    AndroidPreferencesManager.loadHttpCredentials(requireContext());
            if (credentials != null && !credentials.isManuallySet()) {
                credentials.setApiServer(endpoint.baseUrl());
                AndroidPreferencesManager.storeHttpCredentials(requireContext(), credentials);
                mainActivityViewModel.setCredentials(credentials);
            }
            updateEndpointSummary(endpointSummary, brokerHostEditText, httpsSwitch,
                    httpPortEditText, httpsPortEditText);
            String message = "Broker address saved.";
            if (credentials != null && hostChanged) {
                message += " If this is a different broker, use Log in again.";
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });

        haBrokerLoginButton.setOnClickListener(v -> navigateToLogin());
        reloginButton.setOnClickListener(v -> navigateToLogin());

        manualSetupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(AccountFragment.this).navigate(R.id.manual_setup_fragment);
            }
        });

        httpLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // If the stored credentials is just manual, simply discard it
                if (mainActivityViewModel.getCredentials().getValue().isManuallySet()) {
                    mainActivityViewModel.setCredentials(null);
                    AndroidPreferencesManager.removeHttpCredentials(requireContext());
                    return;
                }

                // If the stored credentials has been obtained by login, ask for confirmation first
                AlertDialog dialog = new AlertDialog.Builder(requireActivity()).setMessage("Are you sure you want to discard current HTTP credentials?").setTitle("Confirm").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        HttpClientManager.getInstance().asyncLogout(new HttpClientManager.Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                mainActivityViewModel.setCredentials(null);
                                AndroidPreferencesManager.removeHttpCredentials(requireContext());
                                dialog.dismiss();
                            }

                            @Override
                            public void onFailure(Exception ex) {
                                Toast.makeText(requireContext(),"An error occurred while logging out: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                                AndroidPreferencesManager.removeHttpCredentials(requireContext());
                                mainActivityViewModel.setCredentials(null);
                                dialog.dismiss();
                            }
                        });
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
                dialog.show();
            }
        });

        final SwitchMaterial pairingDebugSwitch = root.findViewById(R.id.pairingDebugSwitch);
        final Button copyPairingLogsButton = root.findViewById(R.id.copyPairingLogsButton);
        final Button clearPairingLogsButton = root.findViewById(R.id.clearPairingLogsButton);
        tlsPinSummaryView = root.findViewById(R.id.tlsPinSummary);
        final Button resetTlsPinButton = root.findViewById(R.id.resetTlsPinButton);
        refreshTlsPinSummary();
        resetTlsPinButton.setOnClickListener(v -> {
            ApiCredentials stored =
                    AndroidPreferencesManager.loadHttpCredentials(requireContext());
            if (stored == null || stored.getApiServer() == null) {
                Toast.makeText(requireContext(), "No broker configured.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            AndroidTlsPolicy.clearPin(requireContext(), stored.getApiServer());
            PairingLog.log("tls", "cleared pinned certificate for " + stored.getApiServer());
            refreshTlsPinSummary();
            Toast.makeText(requireContext(), "Pinned certificate cleared.",
                    Toast.LENGTH_SHORT).show();
        });

        pairingDebugSwitch.setChecked(AndroidPreferencesManager.isPairingDebugEnabled(requireContext()));
        PairingLog.setEnabled(pairingDebugSwitch.isChecked());
        pairingDebugSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AndroidPreferencesManager.setPairingDebugEnabled(requireContext(), isChecked);
            PairingLog.setEnabled(isChecked);
            PairingLog.log("app", "pairing logs " + (isChecked ? "enabled" : "disabled"));
        });

        copyPairingLogsButton.setOnClickListener(v -> {
            String logs = PairingLog.dump();
            ClipboardManager clipboard = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Meross Local Pairer log", logs));
            Toast.makeText(requireContext(), "Pairing logs copied to clipboard.",
                    Toast.LENGTH_SHORT).show();
        });

        clearPairingLogsButton.setOnClickListener(v -> {
            PairingLog.clear();
            PairingLog.log("app", "pairing logs cleared");
            Toast.makeText(requireContext(), "Pairing logs cleared.", Toast.LENGTH_SHORT).show();
        });

        final TextView allowedWifiSummary = root.findViewById(R.id.allowedWifiSummary);
        allowedWifiSummaryView = allowedWifiSummary;
        final Button allowCurrentWifiButton = root.findViewById(R.id.allowCurrentWifiButton);
        refreshAllowedWifiSummary(allowedWifiSummary);

        allowCurrentWifiButton.setOnClickListener(v ->
                allowCurrentWifi(allowedWifiSummary));

        manageAllowedWifiButton.setOnClickListener(v ->
                showManageWifiDialog(allowedWifiSummary));

        return root;
    }

    private void navigateToLogin() {
        Bundle args = new Bundle();
        args.putBoolean(LoginFragment.Args.ENABLE_BROKER_DISCOVERY, true);
        args.putString(LoginFragment.Args.HTTP_BROKER_EMAIL, Constants.HA_ADDON_DEFAULT_EMAIL);
        args.putString(LoginFragment.Args.HTTP_BROKER_PASSWORD, Constants.HA_ADDON_DEFAULT_PASSWORD);
        args.putInt(LoginFragment.Args.INTRO_TEXT_RESOURCE_ID, R.string.login_intro_ha_broker);
        args.putBoolean(LoginFragment.Args.REQUIRES_WIFI_LOCATION, true);
        NavHostFragment.findNavController(AccountFragment.this)
                .navigate(R.id.login_fragment, args);
    }

    private void applyAdvancedVisibility(View root) {
        boolean advanced = AndroidPreferencesManager.isAdvancedMode(requireContext());
        int visibility = advanced ? View.VISIBLE : View.GONE;
        root.findViewById(R.id.advancedBrokerFields).setVisibility(visibility);
        root.findViewById(R.id.manageAllowedWifiButton).setVisibility(visibility);
        View diagnosticsHeader = root.findViewById(R.id.diagnosticsSectionHeader);
        diagnosticsHeader.setVisibility(visibility);
        if (!advanced) {
            root.findViewById(R.id.diagnosticsCard).setVisibility(View.GONE);
            ((TextView) root.findViewById(R.id.diagnosticsSectionArrow)).setText(R.string.text_chevron_collapsed);
        }
        ApiCredentials credentials =
                AndroidPreferencesManager.loadHttpCredentials(requireContext());
        root.findViewById(R.id.setManualButton).setVisibility(
                advanced && credentials == null ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.reloginButton).setVisibility(
                credentials != null ? View.VISIBLE : View.GONE);
    }

    private static BrokerEndpoint readEndpoint(EditText hostField, SwitchMaterial httpsSwitch,
                                               EditText httpPortField,
                                               EditText httpsPortField) {
        return new BrokerEndpoint(
                BrokerEndpoint.sanitizeHost(hostField.getText().toString()),
                httpsSwitch.isChecked(),
                parsePort(httpPortField, BrokerEndpoint.DEFAULT_HTTP_PORT),
                parsePort(httpsPortField, BrokerEndpoint.DEFAULT_HTTPS_PORT));
    }

    private static int parsePort(EditText field, int fallback) {
        try {
            int value = Integer.parseInt(field.getText().toString().trim());
            return value >= 1 && value <= 65535 ? value : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void updateEndpointSummary(TextView summary, EditText hostField,
                                              SwitchMaterial httpsSwitch,
                                              EditText httpPortField,
                                              EditText httpsPortField) {
        BrokerEndpoint endpoint =
                readEndpoint(hostField, httpsSwitch, httpPortField, httpsPortField);
        if (!endpoint.isValid()) {
            summary.setText(R.string.endpoint_enter_address);
        } else if (endpoint.requiresHttps()) {
            summary.setText(R.string.endpoint_https_required);
        } else {
            summary.setText(summary.getContext().getString(
                    R.string.endpoint_format, endpoint.baseUrl()));
        }
    }

    private String[] wifiPermissions() {
        if (Build.VERSION.SDK_INT >= 37) {
            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.ACCESS_LOCAL_NETWORK};
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES};
        }
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    private boolean hasWifiPermissions() {
        for (String permission : wifiPermissions()) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean ensureWifiReady(Runnable action) {
        if (!hasWifiPermissions()) {
            pendingWifiAction = action;
            requestPermissions(wifiPermissions(), REQUEST_WIFI_PERMISSIONS);
            return false;
        }
        if (!AndroidUtils.isLocationEnabled(requireContext())) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Location services required")
                    .setMessage("Android exposes Wi-Fi network names only when Location "
                            + "services are enabled. Enable them, then try again.")
                    .setPositiveButton("Open settings", (dialog, which) ->
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Cancel", null)
                    .show();
            return false;
        }
        return true;
    }

    private void allowCurrentWifi(TextView summary) {
        if (!ensureWifiReady(() -> allowCurrentWifi(summary))) {
            return;
        }
        AllowedWifiNetworks.Entry current = AllowedWifiNetworks.current(requireContext());
        if (current == null) {
            Toast.makeText(requireContext(),
                    "Unable to read the current Wi-Fi network. Check that Wi-Fi is connected "
                            + "and that Wi-Fi/Location permissions are granted.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        AllowedWifiNetworks.add(requireContext(), current.ssid, current.bssid);
        AndroidPreferencesManager.clearWifiDeclined(requireContext(), current.ssid);
        PairingLog.log("network", "allowed current " + current.ssid + "/" + current.bssid);
        refreshAllowedWifiSummary(summary);
        Toast.makeText(requireContext(), "Allowed " + current.ssid, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    private void showManageWifiDialog(TextView summary) {
        // ensureWifiReady() has already checked/requested the Wi-Fi scan
        // permissions; the scan results calls are also failure-tolerant.
        if (!ensureWifiReady(() -> showManageWifiDialog(summary))) {
            return;
        }
        android.net.wifi.WifiManager manager = (android.net.wifi.WifiManager)
                requireContext().getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
        if (manager == null) {
            buildManageWifiDialog(summary, new java.util.ArrayList<>());
            return;
        }
        try {
            manager.startScan();
        } catch (Exception e) {
            PairingLog.log("network", "scan unavailable: " + e.getMessage());
        }
        // Give Android time to refresh the scan results before listing them.
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            List<android.net.wifi.ScanResult> results;
            try {
                results = manager.getScanResults();
            } catch (Exception e) {
                results = new java.util.ArrayList<>();
            }
            buildManageWifiDialog(summary, results);
        }, 2000);
    }

    private void buildManageWifiDialog(TextView summary,
                                       List<android.net.wifi.ScanResult> scanResults) {
        java.util.LinkedHashMap<String, AllowedWifiNetworks.Entry> bySsid =
                new java.util.LinkedHashMap<>();
        for (AllowedWifiNetworks.Entry entry : AllowedWifiNetworks.load(requireContext())) {
            bySsid.put(entry.ssid, entry);
        }
        for (android.net.wifi.ScanResult result : scanResults) {
            String ssid = result.SSID;
            if (ssid == null || ssid.trim().isEmpty() || bySsid.containsKey(ssid)) {
                continue;
            }
            bySsid.put(ssid, new AllowedWifiNetworks.Entry(ssid, result.BSSID));
        }
        if (bySsid.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No visible Wi-Fi network. Enable Wi-Fi/Location and try again.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        List<AllowedWifiNetworks.Entry> entries = new java.util.ArrayList<>(bySsid.values());
        String[] labels = new String[entries.size()];
        boolean[] checked = new boolean[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            AllowedWifiNetworks.Entry entry = entries.get(i);
            labels[i] = entry.label();
            checked[i] = AllowedWifiNetworks.isAllowed(requireContext(), entry.ssid, entry.bssid);
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Allowed Wi-Fi networks")
                .setMultiChoiceItems(labels, checked,
                        (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Save", (dialog, which) -> {
                    List<AllowedWifiNetworks.Entry> desired = new java.util.ArrayList<>();
                    for (int i = 0; i < entries.size(); i++) {
                        if (checked[i]) {
                            desired.add(entries.get(i));
                        }
                    }
                    for (AllowedWifiNetworks.Entry existing :
                            AllowedWifiNetworks.load(requireContext())) {
                        boolean keep = false;
                        for (AllowedWifiNetworks.Entry wanted : desired) {
                            if (wanted.ssid.equals(existing.ssid)) {
                                keep = true;
                                break;
                            }
                        }
                        if (!keep) {
                            AllowedWifiNetworks.remove(requireContext(), existing);
                        }
                    }
                    for (AllowedWifiNetworks.Entry entry : desired) {
                        AllowedWifiNetworks.add(requireContext(), entry.ssid, entry.bssid);
                        AndroidPreferencesManager.clearWifiDeclined(requireContext(), entry.ssid);
                    }
                    refreshAllowedWifiSummary(summary);
                    Toast.makeText(requireContext(), "Allowed networks updated.",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_WIFI_PERMISSIONS) {
            return;
        }
        boolean granted = grantResults.length > 0;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                granted = false;
            }
        }
        if (granted && pendingWifiAction != null) {
            Runnable action = pendingWifiAction;
            pendingWifiAction = null;
            action.run();
        } else {
            Toast.makeText(requireContext(),
                    "Wi-Fi and Location permissions are required to validate networks.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setAccountSection(View root) {
        TextView arrow = root.findViewById(R.id.accountSectionArrow);
        arrow.setText(accountSectionExpanded ? "▾" : "▸");
        View httpCard = root.findViewById(R.id.httpInfoCard);
        View loginCard = root.findViewById(R.id.loginCard);
        if (!accountSectionExpanded) {
            httpCard.setVisibility(View.GONE);
            loginCard.setVisibility(View.GONE);
            return;
        }
        boolean hasCredentials =
                AndroidPreferencesManager.loadHttpCredentials(requireContext()) != null;
        httpCard.setVisibility(hasCredentials ? View.VISIBLE : View.GONE);
        loginCard.setVisibility(hasCredentials ? View.GONE : View.VISIBLE);
    }

    private void setupCardSection(View root, int headerId, int arrowId, int contentId,
                                  boolean initiallyExpanded) {
        View header = root.findViewById(headerId);
        View content = root.findViewById(contentId);
        TextView arrow = root.findViewById(arrowId);
        content.setVisibility(initiallyExpanded ? View.VISIBLE : View.GONE);
        arrow.setText(initiallyExpanded ? "▾" : "▸");
        header.setOnClickListener(v -> {
            boolean expand = content.getVisibility() != View.VISIBLE;
            content.setVisibility(expand ? View.VISIBLE : View.GONE);
            arrow.setText(expand ? "▾" : "▸");
        });
    }

    private void refreshAllowedWifiSummary(TextView summary) {
        List<AllowedWifiNetworks.Entry> entries = AllowedWifiNetworks.load(requireContext());
        if (entries.isEmpty()) {
            summary.setText(R.string.text_allowed_networks_blocked);
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (AllowedWifiNetworks.Entry entry : entries) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("• ").append(entry.label());
        }
        AllowedWifiNetworks.Entry current = AllowedWifiNetworks.current(requireContext());
        if (current != null && !AllowedWifiNetworks.isAllowed(requireContext(),
                current.ssid, current.bssid) && !MerossUtils.isMerossAp(current.ssid)) {
            builder.append("\nCurrent: ").append(current.ssid).append(" (not allowed)");
        }
        summary.setText(builder.toString());
    }

    private void refreshTlsPinSummary() {
        if (tlsPinSummaryView == null) {
            return;
        }
        ApiCredentials stored = AndroidPreferencesManager.loadHttpCredentials(requireContext());
        if (stored == null || stored.getApiServer() == null) {
            tlsPinSummaryView.setText(R.string.text_tls_pin_no_broker);
            return;
        }
        if (!stored.getApiServer().toLowerCase(Locale.ROOT).startsWith("https://")) {
            tlsPinSummaryView.setText(R.string.text_tls_pin_plain_http);
            return;
        }
        String pin = AndroidTlsPolicy.pinnedFingerprint(requireContext(), stored.getApiServer());
        tlsPinSummaryView.setText(pin == null
                ? "TLS pin: not pinned yet (accepted on first HTTPS contact)"
                : "TLS pin (SHA-256): " + pin);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)requireActivity()).setWifiLocationWarnRequired(false);
        AllowedWifiNetworks.addListener(allowedWifiListener);
        if (allowedWifiSummaryView != null) {
            refreshAllowedWifiSummary(allowedWifiSummaryView);
        }
        refreshTlsPinSummary();
    }

    @Override
    public void onPause() {
        AllowedWifiNetworks.removeListener(allowedWifiListener);
        super.onPause();
    }

}
