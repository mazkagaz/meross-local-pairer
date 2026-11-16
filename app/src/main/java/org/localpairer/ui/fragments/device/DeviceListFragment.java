package org.localpairer.ui.fragments.device;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.localpairer.AndroidPreferencesManager;
import org.localpairer.NetworkPolicy;
import org.localpairer.R;
import org.localpairer.model.HttpClientManager;
import org.localpairer.merosslib.MerossHttpClient;
import org.localpairer.merosslib.model.http.ApiCredentials;
import org.localpairer.merosslib.model.http.DeviceInfo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Devices.
 */
public class DeviceListFragment extends Fragment {
    private DeviceRecyclerViewAdapter mAdapter;
    private FloatingActionButton mRefreshButton;
    private FloatingActionButton mPairButton;
    private FloatingActionButton mSettingsButton;

    public DeviceListFragment() {
    }

    @SuppressWarnings("unused")
    public static DeviceListFragment newInstance(int columnCount) {
        return new DeviceListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new DeviceRecyclerViewAdapter(new ArrayList<DeviceInfo>(),
                this::confirmDeleteDevice);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);
        RecyclerView recycler_view = view.findViewById(R.id.device_list_view);
        recycler_view.setAdapter(mAdapter);
        mRefreshButton = view.findViewById(R.id.device_list_refresh_fab);
        mRefreshButton.setOnClickListener(this.refreshButtonClickListener);
        mPairButton = view.findViewById(R.id.device_list_pair_fab);
        mPairButton.setOnClickListener(this.pairButtonClickListener);
        mSettingsButton = view.findViewById(R.id.device_list_settings_fab);
        mSettingsButton.setOnClickListener(this.settingsButtonClickListener);
        view.findViewById(R.id.quitButton).setOnClickListener(v -> requireActivity().finishAffinity());
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.device_list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            NavHostFragment.findNavController(this).navigate(R.id.action_devices_to_about);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
    }

    private void updateData() {
        if (!NetworkPolicy.isCurrentAllowed(requireActivity())) {
            NetworkPolicy.ensureCurrentAllowed(requireActivity(), this::updateData);
            return;
        }
        ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(requireContext());
        if (creds == null) {
            Toast.makeText(requireContext(), "Please login first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (creds.getApiServer()==null) {
            Toast.makeText(requireContext(), "No API server specified.", Toast.LENGTH_SHORT).show();
            return;
        }

        mRefreshButton.setEnabled(false);
        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Fetching data.");
        dialog.setCancelable(false);
        dialog.setMessage("Please wait reading devices info...");
        dialog.show();

        HttpClientManager.getInstance().loadFromCredentials(creds);
        HttpClientManager.getInstance().asyncListDevices(new HttpClientManager.Callback<List<DeviceInfo>>() {
            @Override
            public void onSuccess(List<DeviceInfo> result) {
                mAdapter.setItems(result);
                mRefreshButton.setEnabled(true);
                dialog.dismiss();
            }

            @Override
            public void onFailure(Exception result) {
                mRefreshButton.setEnabled(true);
                dialog.dismiss();
                String tlsError = org.localpairer.AndroidTlsPolicy.pinMismatchMessage(result);
                Toast.makeText(requireContext(),
                        tlsError != null ? tlsError : "Failed to obtain device list.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private View.OnClickListener refreshButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateData();
        }
    };

    private View.OnClickListener pairButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            NavHostFragment.findNavController(DeviceListFragment.this)
                    .navigate(R.id.action_devices_to_pair);
        }
    };

    private View.OnClickListener settingsButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            NavHostFragment.findNavController(DeviceListFragment.this)
                    .navigate(R.id.action_devices_to_account);
        }
    };

    private void confirmDeleteDevice(DeviceInfo device) {
        NetworkPolicy.ensureCurrentAllowed(requireActivity(), () -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Forget device")
                    .setMessage("Remove \"" + device.getDevName()
                            + "\" from the broker? The device must be paired again to come back.")
                    .setPositiveButton("Forget", (dialog, which) -> deleteDevice(device))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void deleteDevice(DeviceInfo device) {
        ApiCredentials credentials =
                AndroidPreferencesManager.loadHttpCredentials(requireContext());
        if (credentials == null) {
            Toast.makeText(requireContext(), "No broker credentials.", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                new MerossHttpClient(credentials).deleteDevice(device.getUuid());
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Device forgotten: " + device.getDevName(), Toast.LENGTH_LONG).show();
                    updateData();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                "Could not forget the device: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}