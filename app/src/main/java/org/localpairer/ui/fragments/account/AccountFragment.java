package org.localpairer.ui.fragments.account;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import org.localpairer.AllowedWifiNetworks;
import org.localpairer.AndroidPreferencesManager;
import org.localpairer.Constants;
import org.localpairer.MainActivity;
import org.localpairer.MerossUtils;
import org.localpairer.PairingLog;
import org.localpairer.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import org.localpairer.model.HttpClientManager;
import org.localpairer.ui.MainActivityViewModel;
import org.localpairer.ui.fragments.login.LoginFragment;
import org.localpairer.merosslib.model.http.ApiCredentials;

import org.eclipse.paho.client.mqttv3.util.Strings;

import java.util.List;

public class AccountFragment extends Fragment {
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final MainActivityViewModel mainActivityViewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);
        View root = inflater.inflate(R.layout.fragment_account, container, false);
        final CardView httpInfoCard = root.findViewById(R.id.httpInfoCard);
        final EditText httpUrlEditText = root.findViewById(R.id.httpUrlEditText);
        final EditText userIdEditText = root.findViewById(R.id.userIdEditText);
        final EditText httpTokenEditText = root.findViewById(R.id.httpTokenEditText);
        final EditText mqttKeyEditText = root.findViewById(R.id.mqttKeyEditText);
        final Button httpLogoutButton = root.findViewById(R.id.httpLogoutButton);
        final Button accountBackToDevicesButton = root.findViewById(R.id.accountBackToDevicesButton);
        final Button haBrokerLoginButton = root.findViewById(R.id.haBrokerLoginButton);
        final CardView loginCardView = root.findViewById(R.id.loginCard);
        final Button manualSetupButton = root.findViewById(R.id.setManualButton);
        final ImageView loggedInAccountLogoImageView = root.findViewById(R.id.loggedInAccountLogo);

        mainActivityViewModel.getCredentials().observe(getViewLifecycleOwner(), new Observer<ApiCredentials>() {
            @Override
            public void onChanged(ApiCredentials apiCredentials) {
                if (apiCredentials == null || Strings.isEmpty(apiCredentials.getApiServer())) {
                    loggedInAccountLogoImageView.setImageResource(R.drawable.question_mark);
                } else {
                    loggedInAccountLogoImageView.setImageResource(R.drawable.ha_logo);
                }

                if (apiCredentials == null || Strings.isEmpty(apiCredentials.getApiServer())) {
                    httpUrlEditText.setText("Not set");
                } else {
                    httpUrlEditText.setText(apiCredentials.getApiServer());
                }

                if (apiCredentials == null || Strings.isEmpty(apiCredentials.getUserId())) {
                    userIdEditText.setText("Not set");
                } else {
                    userIdEditText.setText(apiCredentials.getUserId());
                }

                if (apiCredentials == null || Strings.isEmpty(apiCredentials.getToken())) {
                    httpTokenEditText.setText("Not set");
                } else {
                    httpTokenEditText.setText(apiCredentials.getToken());
                }

                if (apiCredentials == null || Strings.isEmpty(apiCredentials.getKey())) {
                    mqttKeyEditText.setText("Not set (empty string)");
                } else {
                    mqttKeyEditText.setText(apiCredentials.getKey());
                }

                if (apiCredentials != null && apiCredentials.isManuallySet()) {
                    httpLogoutButton.setText("Discard");
                } else {
                    httpLogoutButton.setText("Logout");
                }

                httpLogoutButton.setEnabled(apiCredentials != null);
                httpInfoCard.setVisibility(apiCredentials == null ? View.GONE : View.VISIBLE);
                loginCardView.setVisibility(apiCredentials == null ? View.VISIBLE : View.GONE);
                haBrokerLoginButton.setEnabled(apiCredentials == null);
                manualSetupButton.setVisibility(apiCredentials == null ? View.VISIBLE : View.GONE);
            }
        });

        haBrokerLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putBoolean(LoginFragment.Args.ENABLE_BROKER_DISCOVERY, true);
                args.putString(LoginFragment.Args.HTTP_BROKER_EMAIL, Constants.HA_ADDON_DEFAULT_EMAIL);
                args.putString(LoginFragment.Args.HTTP_BROKER_PASSWORD, Constants.HA_ADDON_DEFAULT_PASSWORD);
                args.putInt(LoginFragment.Args.INTRO_TEXT_RESOURCE_ID, R.string.login_intro_ha_broker);
                args.putBoolean(LoginFragment.Args.REQUIRES_WIFI_LOCATION, true);
                NavHostFragment.findNavController(AccountFragment.this).navigate(R.id.login_fragment, args);
            }
        });
        accountBackToDevicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(AccountFragment.this)
                        .navigate(R.id.action_account_to_devices);
            }
        });

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
        final Button allowCurrentWifiButton = root.findViewById(R.id.allowCurrentWifiButton);
        final Button manageAllowedWifiButton = root.findViewById(R.id.manageAllowedWifiButton);
        refreshAllowedWifiSummary(allowedWifiSummary);

        allowCurrentWifiButton.setOnClickListener(v -> {
            AllowedWifiNetworks.Entry current = AllowedWifiNetworks.current(requireContext());
            if (current == null) {
                Toast.makeText(requireContext(),
                        "Connect to a Wi-Fi network first.", Toast.LENGTH_LONG).show();
                return;
            }
            AllowedWifiNetworks.add(requireContext(), current.ssid, current.bssid);
            PairingLog.log("network", "allowed current " + current.ssid + "/" + current.bssid);
            refreshAllowedWifiSummary(allowedWifiSummary);
        });

        manageAllowedWifiButton.setOnClickListener(v -> {
            List<AllowedWifiNetworks.Entry> entries =
                    AllowedWifiNetworks.load(requireContext());
            if (entries.isEmpty()) {
                Toast.makeText(requireContext(),
                        "No allowed network yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] labels = new String[entries.size()];
            boolean[] selected = new boolean[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                labels[i] = entries.get(i).label();
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("Select networks to remove")
                    .setMultiChoiceItems(labels, selected,
                            (dialog, which, isChecked) -> selected[which] = isChecked)
                    .setPositiveButton("Remove", (dialog, which) -> {
                        for (int i = entries.size() - 1; i >= 0; i--) {
                            if (selected[i]) {
                                AllowedWifiNetworks.remove(requireContext(), entries.get(i));
                            }
                        }
                        refreshAllowedWifiSummary(allowedWifiSummary);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        return root;
    }

    private void refreshAllowedWifiSummary(TextView summary) {
        List<AllowedWifiNetworks.Entry> entries = AllowedWifiNetworks.load(requireContext());
        if (entries.isEmpty()) {
            summary.setText("No network validated. Broker traffic is blocked.");
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

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)requireActivity()).setWifiLocationWarnRequired(false);
    }

}
