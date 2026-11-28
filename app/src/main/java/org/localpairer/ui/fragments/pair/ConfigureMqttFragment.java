package org.localpairer.ui.fragments.pair;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import org.localpairer.AndroidPreferencesManager;
import org.localpairer.PairingLog;
import org.localpairer.R;
import org.localpairer.TlsProbe;
import org.localpairer.model.MqttConfiguration;
import org.localpairer.ui.PairActivityViewModel;
import org.localpairer.merosslib.model.http.ApiCredentials;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.List;


public class ConfigureMqttFragment extends Fragment {
    private PairActivityViewModel pairActivityViewModel;
    private TextInputLayout mqttConfigurationNameEditText;
    private TextInputLayout mqttHostEditText;
    private TextInputLayout mqttPortEditText;
    private Spinner mqttConfigurationSpinner;
    private CheckBox saveCheckbox;
    private CheckBox overrideMqttParamsCheckbox;
    private CheckBox modifyDefaultCheckbox;
    private TextInputLayout customMqttUserId;
    private TextInputLayout customMqttKey;
    private MqttConfiguration mDiscoveredConfig;
    private MqttConfiguration defaultConfiguration;
    private ArrayAdapter<MqttConfiguration> adapter;
    private final MqttConfiguration newMqttConfig = new MqttConfiguration("Add new...", null, -1);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);
        parseArgumets();
    }

    private void parseArgumets() {
        Bundle args = getArguments();
        if (args != null) {
            String hostname = args.getString("hostname");
            int port = args.getInt("port", -1);
            if (hostname!=null && port != -1) {
                mDiscoveredConfig = new MqttConfiguration();
                mDiscoveredConfig.setName("Local Broker on " + hostname + ":" + port);
                mDiscoveredConfig.setHostname(hostname);
                mDiscoveredConfig.setPort(port);
            }
        }
    }

    private MqttConfiguration buildDefaultConfiguration() {
        ApiCredentials credentials = AndroidPreferencesManager.loadHttpCredentials(requireContext());
        if (credentials == null) {
            return null;
        }
        String host = extractHost(credentials.getApiServer());
        if (host == null) {
            return null;
        }
        return new MqttConfiguration(getString(R.string.default_broker_name), host, 2001);
    }

    private String extractHost(String apiServer) {
        if (apiServer == null || apiServer.trim().isEmpty()) {
            return null;
        }
        String value = apiServer.trim();
        try {
            URI uri = new URI(value.contains("://") ? value : "http://" + value);
            if (uri.getHost() != null && !uri.getHost().isEmpty()) {
                return uri.getHost();
            }
        } catch (URISyntaxException ignored) {
            // Fall back to manual parsing below.
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

    private void setMqttFieldsEditable(boolean editable) {
        mqttConfigurationNameEditText.setEnabled(editable);
        mqttHostEditText.setEnabled(editable);
        mqttPortEditText.setEnabled(editable);
        mqttPortEditText.setHelperTextEnabled(editable);
        mqttPortEditText.setHelperText(editable
                ? "MQTT over TLS is required. Meross Local Broker uses 2001."
                : null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mqtt_config, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mqttConfigurationSpinner = view.findViewById(R.id.mqttConfigurationSpinner);
        final TextInputLayout deviceNameInputLayout =
                view.findViewById(R.id.deviceNameInputLayout);
        ApiCredentials storedCredentials =
                AndroidPreferencesManager.loadHttpCredentials(requireContext());
        if (storedCredentials != null && storedCredentials.isManuallySet()) {
            // Manual credentials cannot call the broker API to store the name.
            deviceNameInputLayout.setVisibility(View.GONE);
        }
        mqttConfigurationNameEditText = view.findViewById(R.id.mqttConfigurationNameEditText);
        mqttHostEditText = view.findViewById(R.id.mqttHostnameEditText);
        mqttPortEditText = view.findViewById(R.id.mqttPortEditText);
        Button pairButton = view.findViewById(R.id.pairButton);
        saveCheckbox = view.findViewById(R.id.saveCheckbox);
        customMqttUserId = view.findViewById(R.id.customMqttUserId);
        customMqttKey = view.findViewById(R.id.customMqttKey);
        overrideMqttParamsCheckbox = view.findViewById(R.id.overrideMqttParams);
        modifyDefaultCheckbox = view.findViewById(R.id.modifyDefaultCheckbox);

        customMqttKey.setVisibility(View.GONE);
        customMqttUserId.setVisibility(View.GONE);

        List<MqttConfiguration> configurations = AndroidPreferencesManager.loadAllMqttConfigurations(requireContext());
        java.util.Iterator<MqttConfiguration> iterator = configurations.iterator();
        while (iterator.hasNext()) {
            MqttConfiguration c = iterator.next();
            if (c.getName() != null && "default".equalsIgnoreCase(c.getName().trim())) {
                iterator.remove();
            }
        }
        defaultConfiguration = buildDefaultConfiguration();
        if (defaultConfiguration != null) {
            configurations.add(defaultConfiguration);
        }
        configurations.add(this.newMqttConfig);
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, configurations);
        if (mDiscoveredConfig != null) {
            adapter.add(mDiscoveredConfig);
        }
        saveCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mqttConfigurationNameEditText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        overrideMqttParamsCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                customMqttUserId.setVisibility(isChecked ? View.VISIBLE:View.GONE);
                customMqttKey.setVisibility(isChecked ? View.VISIBLE:View.GONE);
            }
        });
        mqttConfigurationSpinner.setAdapter(adapter);

        modifyDefaultCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MqttConfiguration selection = (MqttConfiguration) mqttConfigurationSpinner.getSelectedItem();
                if (selection == defaultConfiguration) {
                    setMqttFieldsEditable(isChecked);
                    saveCheckbox.setVisibility(View.GONE);
                }
            }
        });

        mqttConfigurationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MqttConfiguration selection = (MqttConfiguration) parent.getAdapter().getItem(position);
                modifyDefaultCheckbox.setChecked(false);
                if (selection == newMqttConfig) {
                    mqttConfigurationNameEditText.getEditText().setText("");
                    mqttHostEditText.getEditText().setText("");
                    mqttPortEditText.getEditText().setText("");
                    setMqttFieldsEditable(true);
                    saveCheckbox.setVisibility(View.VISIBLE);
                    modifyDefaultCheckbox.setVisibility(View.GONE);
                } else {
                    mqttConfigurationNameEditText.getEditText().setText(selection.getName());
                    mqttHostEditText.getEditText().setText(selection.getHostname());
                    mqttPortEditText.getEditText().setText(""+selection.getPort());
                    setMqttFieldsEditable(false);
                    saveCheckbox.setVisibility(View.GONE);
                    modifyDefaultCheckbox.setVisibility(selection == defaultConfiguration ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        MqttConfiguration preferred = defaultConfiguration != null ? defaultConfiguration
                : (mDiscoveredConfig != null ? mDiscoveredConfig : newMqttConfig);
        int preferredPosition = adapter.getPosition(preferred);
        mqttConfigurationSpinner.setSelection(preferredPosition >= 0 ? preferredPosition : adapter.getPosition(newMqttConfig), true);

        pairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MqttConfiguration selected = (MqttConfiguration) mqttConfigurationSpinner.getSelectedItem();
                boolean editingNew = selected == newMqttConfig;
                boolean editingDefault = selected == defaultConfiguration && modifyDefaultCheckbox.isChecked();
                final boolean validateTls = editingNew || editingDefault;
                if (editingNew || editingDefault) {
                    MqttConfiguration tmpConf = new MqttConfiguration();
                    boolean save = editingNew && saveCheckbox.isChecked();
                    boolean error = false;

                    // make sure the host is populated
                    String hostnameStr = mqttHostEditText.getEditText().getText().toString().trim();
                    if (hostnameStr.isEmpty()) {
                        error = true;
                        mqttHostEditText.setError("Invalid mqtt host");
                    } else {
                        mqttHostEditText.setError(null);
                        tmpConf.setHostname(hostnameStr);
                    }

                    // make sure the port is populated
                    try {
                        String portstr = mqttPortEditText.getEditText().getText().toString();
                        int port = Integer.parseInt(portstr);
                        if (port<1 || port > 65535)
                            throw new NumberFormatException();
                        mqttPortEditText.setError(null);
                        tmpConf.setPort(port);

                    } catch (NumberFormatException e) {
                        error = true;
                        mqttPortEditText.setError("The MQTT port is invalid");
                    }

                    // Make sure the name is populated
                    String name = mqttConfigurationNameEditText.getEditText().getText().toString().trim();
                    if (save && (name.isEmpty() || name.trim().toLowerCase(Locale.ROOT).compareTo("add new...")==0)) {
                        error = true;
                        mqttConfigurationNameEditText.setError("Invalid name");
                    } else {
                        mqttConfigurationNameEditText.setError(null);
                        tmpConf.setName(name.isEmpty() ? getString(R.string.default_broker_name) : name);
                    }

                    // Abort on validation error
                    if (error) {
                        return;
                    }

                    if (save) {
                        AndroidPreferencesManager.storeNewMqttConfiguration(getContext(), tmpConf);
                        adapter.insert(tmpConf,0);
                        adapter.notifyDataSetChanged();
                        mqttConfigurationSpinner.setSelection(0);
                    }

                    pairActivityViewModel.setTargetMqttConfig(tmpConf);
                    PairingLog.log("broker", "selected edited broker name=" + tmpConf.getName()
                            + " host=" + tmpConf.getHostname() + " port=" + tmpConf.getPort());
                } else {
                    pairActivityViewModel.setTargetMqttConfig(selected);
                    PairingLog.log("broker", "selected name=" + selected.getName()
                            + " host=" + selected.getHostname() + " port=" + selected.getPort()
                            + " default=" + (selected == defaultConfiguration));
                }

                if (overrideMqttParamsCheckbox.isChecked()) {
                    String overrideKey = customMqttKey.getEditText().getText().toString().trim();
                    String overrideUserId = customMqttUserId.getEditText().getText().toString().trim();
                    if (overrideKey.isEmpty()) {
                        customMqttKey.setError("The User Key cannot be empty");
                        return;
                    }
                    if (overrideUserId.isEmpty()) {
                        customMqttUserId.setError("The UserId cannot be empty");
                        return;
                    }
                    try {
                        if (Integer.parseInt(overrideUserId) < 0)
                            throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        customMqttUserId.setError("UserId must be a positive integer");
                        return;
                    }
                    customMqttKey.setError(null);
                    customMqttUserId.setError(null);
                    pairActivityViewModel.setOverrideKey(overrideKey);
                    pairActivityViewModel.setOverrideUserId(overrideUserId);
                } else {
                    pairActivityViewModel.setOverrideKey(null);
                    pairActivityViewModel.setOverrideUserId(null);
                }

                String deviceName = "";
                if (deviceNameInputLayout.getVisibility() == View.VISIBLE) {
                    deviceName = deviceNameInputLayout.getEditText()
                            .getText().toString().trim();
                    if (deviceName.isEmpty()) {
                        deviceNameInputLayout.setError("Please input a device name");
                        return;
                    }
                    if (deviceName.length() > 128) {
                        deviceNameInputLayout.setError("Device name is too long");
                        return;
                    }
                    deviceNameInputLayout.setError(null);
                }
                pairActivityViewModel.setTargetDeviceName(
                        deviceName.isEmpty() ? null : deviceName);

                finishConfiguration(validateTls);
            }
        });
    }

    private void finishConfiguration(boolean validateTls) {
        MqttConfiguration target = pairActivityViewModel.getTargetMqttConfig().getValue();
        if (!validateTls || target == null) {
            navigateToPairing();
            return;
        }
        final String host = target.getHostname();
        final int port = target.getPort();
        final android.app.Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        final ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Checking broker security");
        dialog.setCancelable(false);
        dialog.setMessage("Checking that the broker accepts MQTT over TLS...");
        dialog.show();

        new Thread(() -> {
            final TlsProbe.Result result = TlsProbe.probe(host, port);
            activity.runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                dialog.dismiss();
                if (result == TlsProbe.Result.OK) {
                    navigateToPairing();
                } else if (result == TlsProbe.Result.NOT_TLS) {
                    mqttPortEditText.setError("This broker does not accept MQTT over TLS. "
                            + "Meross devices require TLS; use the TLS port "
                            + "(Meross Local Broker: 2001).");
                } else {
                    mqttHostEditText.setError("Broker unreachable at " + host + ":" + port);
                }
            });
        }).start();
    }

    private void navigateToPairing() {
        NavController ctrl = NavHostFragment.findNavController(ConfigureMqttFragment.this);
        ctrl.navigate(R.id.action_configureMqtt_to_executePair, null,
                new NavOptions.Builder().setEnterAnim(android.R.animator.fade_in)
                        .setExitAnim(android.R.animator.fade_out).build());
    }
}
