package org.localpairer.ui.fragments.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import org.localpairer.AndroidPreferencesManager;
import org.localpairer.AndroidUtils;
import org.localpairer.BrokerDiscovery;
import org.localpairer.BrokerEndpoint;
import org.localpairer.MainActivity;
import org.localpairer.NetworkPolicy;
import org.localpairer.R;
import org.localpairer.model.HttpClientManager;
import org.localpairer.ui.MainActivityViewModel;
import org.localpairer.merosslib.model.http.ApiCredentials;
import org.localpairer.merosslib.model.http.ErrorCodes;
import org.localpairer.merosslib.model.http.exceptions.HttpApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static org.localpairer.AndroidUtils.dpToPx;


public class LoginFragment extends Fragment {
    // Constants
    private static final String SERVICE_TYPE = "_meross-api._tcp";
    private static final  String TAG = "Login";

    // Instance attributes
    private Context mAppContext = null;
    private boolean mDiscoveryInProgress = false;
    private boolean mResolutionInProgress = false;
    private boolean mDiscoveryEnabled = false;
    private boolean mRequiresWifiLocation = false;
    private NsdManager mNsdManager;
    private TextInputLayout mHttpHostnameInputLayout;
    private EditText mHttpHostnameEditText;
    private EditText mHttpUsernameEditText;
    private EditText mHttpPasswordEditText;
    private SwitchMaterial mHttpsSwitch;
    private LinearLayout mAdvancedEndpointFields;
    private TextInputLayout mHttpPortInputLayout;
    private TextInputLayout mHttpsPortInputLayout;
    private EditText mHttpPortEditText;
    private EditText mHttpsPortEditText;
    private TextView mEndpointSummary;
    private MaterialButton mLoginButton;
    private TextView mDiscoveryHint;
    private TextView httpLoginIntroText;

    private Timer mTimer;
    private Handler mUiHandler;

    private CircularProgressIndicator mSearchProgress;
    private IndeterminateDrawable<CircularProgressIndicatorSpec> mProgressIndicatorDrawable;

    public static class Args {
        public static final String HTTP_BROKER_URL = "org.localpairer.ui.fragments.account.AccountFragment.Args.HTTP_BROKER_URL";
        public static final String HTTP_BROKER_EMAIL = "org.localpairer.ui.fragments.account.AccountFragment.Args.HTTP_BROKER_EMAIL";
        public static final String HTTP_BROKER_PASSWORD = "org.localpairer.ui.fragments.account.AccountFragment.Args.HTTP_BROKER_PASSWORD";
        public static final String ENABLE_BROKER_DISCOVERY = "org.localpairer.ui.fragments.account.AccountFragment.Args.ENABLE_BROKER_DISCOVERY";
        public static final String INTRO_TEXT_RESOURCE_ID = "org.localpairer.ui.fragments.account.AccountFragment.Args.INTRO_TEXT_RESOURCE_ID";
        public static final String REQUIRES_WIFI_LOCATION = "org.localpairer.ui.fragments.account.AccountFragment.Args.REQUIRES_WIFI_LOCATION";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppContext = requireContext().getApplicationContext();
        mNsdManager = (NsdManager) requireContext().getSystemService(Context.NSD_SERVICE);
        mUiHandler = new Handler(this.requireContext().getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Bind views
        CheckBox showPasswordCheckBox = view.findViewById(R.id.showPasswordCheckBox);
        mHttpHostnameInputLayout =  view.findViewById(R.id.httpHostnameInputLayout);
        mHttpHostnameEditText = mHttpHostnameInputLayout.getEditText();
        mHttpPasswordEditText = ((TextInputLayout)view.findViewById(R.id.httpPasswordEditText)).getEditText();
        mHttpUsernameEditText = ((TextInputLayout)view.findViewById(R.id.httpUsernameEditText)).getEditText();
        mHttpsSwitch = view.findViewById(R.id.httpsSwitch);
        mAdvancedEndpointFields = view.findViewById(R.id.advancedEndpointFields);
        mHttpPortInputLayout = view.findViewById(R.id.httpPortInputLayout);
        mHttpsPortInputLayout = view.findViewById(R.id.httpsPortInputLayout);
        mHttpPortEditText = mHttpPortInputLayout.getEditText();
        mHttpsPortEditText = mHttpsPortInputLayout.getEditText();
        mEndpointSummary = view.findViewById(R.id.endpointSummary);
        mLoginButton = view.findViewById(R.id.loginButton);
        mDiscoveryHint = view.findViewById(R.id.hintForDiscovery);
        httpLoginIntroText = view.findViewById(R.id.httpLoginIntroText);

        // Configure HostEditText for progress showing
        mSearchProgress = new CircularProgressIndicator(requireContext(), null);
        mSearchProgress.setIndicatorSize((int)dpToPx(requireContext(), 15));
        mSearchProgress.setIndeterminate(true);
        mProgressIndicatorDrawable = mSearchProgress.getIndeterminateDrawable();
        // Show/Hide password logic
        showPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mHttpPasswordEditText.getTransformationMethod() == HideReturnsTransformationMethod.getInstance()) {
                    mHttpPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    mHttpPasswordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            }
        });

        // Login button logic
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Make sure wifi is currently enabled and connected.
                if (mRequiresWifiLocation && !AndroidUtils.isWifiConnected(requireContext())) {
                    Snackbar.make(requireView(), "Please enable WiFi first.", Snackbar.LENGTH_SHORT).show();
                } else {
                    performLogin();
                }
            }
        });

        // Setup the edit-text to hide the password characters
        showPasswordCheckBox.setChecked(false);
        mHttpPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());

        // Configure the UI based on received args, if any.
        Bundle args = getArguments();
        if (args == null)
            args = new Bundle();

        mRequiresWifiLocation = args.getBoolean(Args.REQUIRES_WIFI_LOCATION, false);
        mDiscoveryEnabled = args.getBoolean(Args.ENABLE_BROKER_DISCOVERY, false);

        BrokerEndpoint endpoint = AndroidPreferencesManager.loadBrokerEndpoint(requireContext());
        String legacyUrl = args.getString(Args.HTTP_BROKER_URL, "");
        if (!legacyUrl.isEmpty()) {
            BrokerEndpoint parsed = BrokerEndpoint.fromUrl(legacyUrl);
            if (parsed != null) {
                endpoint = parsed;
            }
        }
        mHttpHostnameEditText.setText(endpoint.getHost());
        mHttpsSwitch.setChecked(endpoint.isHttps());
        mHttpPortEditText.setText(String.valueOf(endpoint.getHttpPort()));
        mHttpsPortEditText.setText(String.valueOf(endpoint.getHttpsPort()));
        boolean advanced = AndroidPreferencesManager.isAdvancedMode(requireContext());
        mAdvancedEndpointFields.setVisibility(advanced ? View.VISIBLE : View.GONE);
        updateEndpointSummary();

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateEndpointSummary();
            }
        };
        mHttpHostnameEditText.addTextChangedListener(watcher);
        mHttpPortEditText.addTextChangedListener(watcher);
        mHttpsPortEditText.addTextChangedListener(watcher);
        mHttpsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateEndpointSummary());

        mHttpUsernameEditText.setText(args.getString(Args.HTTP_BROKER_EMAIL, ""));
        mHttpPasswordEditText.setText(args.getString(Args.HTTP_BROKER_PASSWORD, ""));
        httpLoginIntroText.setText(args.getInt(Args.INTRO_TEXT_RESOURCE_ID, R.string.login_intro_text));

        if (mDiscoveryEnabled) {
            setDiscoveryHint("Looking for the local broker on this network…");
            if (!mRequiresWifiLocation || AndroidUtils.isWifiConnected(requireContext())) {
                mUiHandler.post(this::startApiDiscovery);
            } else {
                setDiscoveryHint("Connect to Wi-Fi to search for the local broker.");
            }
        } else {
            mDiscoveryHint.setVisibility(View.GONE);
        }

        return view;
    }

    private BrokerEndpoint readEndpoint() {
        String host = BrokerEndpoint.sanitizeHost(mHttpHostnameEditText.getText().toString());
        return new BrokerEndpoint(
                host,
                mHttpsSwitch.isChecked(),
                parsePort(mHttpPortEditText, BrokerEndpoint.DEFAULT_HTTP_PORT),
                parsePort(mHttpsPortEditText, BrokerEndpoint.DEFAULT_HTTPS_PORT));
    }

    private static int parsePort(EditText field, int fallback) {
        try {
            int value = Integer.parseInt(field.getText().toString().trim());
            return value >= 1 && value <= 65535 ? value : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private void updateEndpointSummary() {
        if (mEndpointSummary == null) {
            return;
        }
        BrokerEndpoint endpoint = readEndpoint();
        mEndpointSummary.setText(endpoint.isValid()
                ? "Endpoint: " + endpoint.baseUrl()
                : "Endpoint: enter a Home Assistant address");
    }

    private void startApiDiscovery() {
        // Do not disable manual entry while the automatic search runs.
        mResolutionInProgress = false;
        configureUi(true, true, null, null);
        setDiscoveryHint("Looking for the local broker on this network…");

        // Start mDNS discovery
        if (!mDiscoveryInProgress) {
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        }

        // Start a timer for aborting discovery after 10 seconds if nothing is found.
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new TimeoutTask(), 5000);
        }
    }

    @Override
    public void onDestroy() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        if (mDiscoveryInProgress)
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);

        super.onDestroy();
    }

    private void configureUi(final boolean uiEnabled, final boolean discoveryInProgress, @Nullable final String hostnameValue, @Nullable final String message) {
        Runnable logic = new Runnable() {
            @Override
            public void run() {
                mLoginButton.setEnabled(uiEnabled);
                mHttpUsernameEditText.setEnabled(uiEnabled);
                mHttpPasswordEditText.setEnabled(uiEnabled);

                if (discoveryInProgress) {
                    mHttpHostnameInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                    mHttpHostnameInputLayout.setEndIconDrawable(mProgressIndicatorDrawable);
                } else {
                    mHttpHostnameInputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
                    mHttpHostnameInputLayout.setEndIconDrawable(null);
                }

                if (hostnameValue != null) {
                    mHttpHostnameEditText.setText(hostnameValue);
                }

                if (message != null) {
                    Toast.makeText(mAppContext, message, Toast.LENGTH_SHORT).show();
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            logic.run();
        } else {
            mUiHandler.post(logic);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)requireActivity()).setWifiLocationWarnRequired(mRequiresWifiLocation);
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            if (mDiscoveryEnabled)
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop service discovery");
        }
    }

    private void performLogin() {
        if (!NetworkPolicy.isCurrentAllowed(requireActivity())) {
            NetworkPolicy.ensureCurrentAllowed(requireActivity(), this::performLogin);
            return;
        }
        // Validate inputs.
        final BrokerEndpoint endpoint = readEndpoint();
        if (!endpoint.isValid()) {
            mHttpHostnameEditText.setError("Please input the Home Assistant address");
            return;
        }
        mHttpHostnameEditText.setError(null);
        AndroidPreferencesManager.saveBrokerEndpoint(requireContext(), endpoint);
        final String strurl = endpoint.baseUrl();

        // Validate username
        final String username = mHttpUsernameEditText.getText().toString().trim();
        if (username.isEmpty()) {
            mHttpUsernameEditText.setError("Please input your username/email");
            return;
        }
        mHttpUsernameEditText.setError(null);

        // Validate password
        final String password = mHttpPasswordEditText.getText().toString().trim();
        if (password.isEmpty()) {
            mHttpPasswordEditText.setError("Please input your password");
            return;
        }
        mHttpPasswordEditText.setError(null);

        // Execute the login.
        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Logging in");
        dialog.setCancelable(false);
        dialog.setMessage("Please wait while logging in...");
        dialog.show();

        HttpClientManager.getInstance().asyncLogin(strurl, username, password, new HttpClientManager.Callback<ApiCredentials>() {
            @Override
            public void onSuccess(ApiCredentials creds) {
                AndroidPreferencesManager.storeHttpCredentials(requireContext(), creds);
                dialog.dismiss();
                MainActivityViewModel mainActivityViewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);
                mainActivityViewModel.setCredentials(creds);
                NavHostFragment.findNavController(LoginFragment.this).navigate(
                        R.id.devices_fragment, null,
                        new NavOptions.Builder()
                                .setPopUpTo(R.id.account_fragment, true)
                                .build());
            }

            @Override
            public void onFailure(Exception exception) {
                dialog.dismiss();
                Log.e(TAG, "Error occurred", exception);
                String errorMessage = "An error occurred while executing the request.";
                String tlsError = org.localpairer.AndroidTlsPolicy.pinMismatchMessage(exception);
                if (tlsError != null) {
                    errorMessage = tlsError;
                }
                else if (exception instanceof HttpApiException) {
                    errorMessage = ((HttpApiException)exception).getErrorMessage();
                }
                Snackbar.make(mLoginButton,errorMessage , Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // mDNS discovery listener
    private final NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {
        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.d(TAG, "Service discovery started");
            mDiscoveryInProgress = true;
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            Log.d(TAG, "Service discovery success" + service);
            mNsdManager.resolveService(service, mResolveListener);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            configureUi(true,  false, null, "Discovery failed");
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
            mDiscoveryInProgress = false;
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "service lost: " + serviceInfo.getServiceType());
        }
    };

    // mDNS resolver
    private final NsdManager.ResolveListener mResolveListener = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed" + errorCode);
            configureUi(true,  false, null, "Resolve failed");
        }
        @Override
        public void onServiceResolved(final NsdServiceInfo serviceInfo) {
            if (mResolutionInProgress) {
                return;
            }
            mResolutionInProgress = true;
            Log.i(TAG, "Found local API! " + serviceInfo);

            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
            if (mDiscoveryInProgress) {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            }

            Map<String, byte[]> attributes = Collections.emptyMap();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && serviceInfo.getAttributes() != null) {
                attributes = serviceInfo.getAttributes();
            }
            String hostTxt = BrokerDiscovery.decode(attributes, "host");
            String httpTxt = BrokerDiscovery.decode(attributes, "http_port");
            String httpsTxt = BrokerDiscovery.decode(attributes, "https_port");
            boolean hasTxt = hostTxt != null || httpTxt != null || httpsTxt != null;

            String fallback = serviceInfo.getHost() != null
                    ? serviceInfo.getHost().getHostAddress() : null;
            final List<String> candidates = BrokerDiscovery.hostCandidates(hostTxt, fallback);
            final int httpPort = BrokerDiscovery.parsePort(
                    httpTxt, BrokerEndpoint.DEFAULT_HTTP_PORT);
            final int advertisedHttpsPort = hasTxt
                    ? BrokerDiscovery.parsePort(httpsTxt, 0)
                    : BrokerEndpoint.DEFAULT_HTTPS_PORT;
            final int httpsPortForUi = advertisedHttpsPort > 0
                    ? advertisedHttpsPort : BrokerEndpoint.DEFAULT_HTTPS_PORT;
            final boolean preferHttps = hasTxt && advertisedHttpsPort > 0;
            final boolean updateEndpoint = hasTxt;
            final int probePort = preferHttps ? advertisedHttpsPort : httpPort;

            new Thread(() -> {
                String chosen = null;
                boolean useHttps = preferHttps;
                for (String candidate : candidates) {
                    if (reachable(candidate, probePort)) {
                        chosen = candidate;
                        break;
                    }
                }
                if (chosen == null && preferHttps && httpPort > 0) {
                    for (String candidate : candidates) {
                        if (reachable(candidate, httpPort)) {
                            chosen = candidate;
                            useHttps = false;
                            break;
                        }
                    }
                }
                final String resultHost = chosen;
                final boolean resultHttps = useHttps;
                mUiHandler.post(() -> {
                    mResolutionInProgress = false;
                    if (resultHost == null) {
                        configureUi(true, false, null,
                                "Local service found, but no advertised address is reachable.");
                        setDiscoveryHint("No reachable local broker found — enter the address manually.");
                        return;
                    }
                    boolean fieldEmpty =
                            mHttpHostnameEditText.getText().toString().trim().isEmpty();
                    if (fieldEmpty) {
                        if (updateEndpoint) {
                            mHttpsSwitch.setChecked(resultHttps);
                            mHttpPortEditText.setText(String.valueOf(httpPort));
                            mHttpsPortEditText.setText(String.valueOf(httpsPortForUi));
                        }
                        configureUi(true, false, resultHost, "Found local API service!");
                        updateEndpointSummary();
                    } else {
                        configureUi(true, false, null, "Found local API service.");
                    }
                    setDiscoveryHint("Local broker found.");
                });
            }).start();
        }
    };

    private static boolean reachable(String host, int port) {
        String address = host;
        if (address.startsWith("[") && address.endsWith("]")) {
            address = address.substring(1, address.length() - 1);
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(address, port), 1200);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void setDiscoveryHint(String text) {
        mUiHandler.post(() -> {
            if (mDiscoveryHint != null) {
                mDiscoveryHint.setText(text);
            }
        });
    }

    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            configureUi(true, false, null, "No Local HTTP service found.");
            setDiscoveryHint("No local broker found — enter the address manually.");
            mTimer = null;
            if (mDiscoveryInProgress)
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
    }
}
