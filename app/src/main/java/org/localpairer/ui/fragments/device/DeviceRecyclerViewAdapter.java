package org.localpairer.ui.fragments.device;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.localpairer.R;
import org.localpairer.databinding.FragmentDeviceInfoBinding;
import org.localpairer.merosslib.model.OnlineStatus;
import org.localpairer.merosslib.model.http.DeviceInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DeviceRecyclerViewAdapter extends RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder> {
    private static final Set<String> ROLLER_SHUTTER_TYPES =
            new HashSet<>(Arrays.asList("mrs100", "mss560", "mss570", "mrs200"));

    public interface OnDeviceActionListener {
        void onDeleteDevice(DeviceInfo device);

        void onRenameDevice(DeviceInfo device);
    }

    private final List<DeviceInfo> mValues;
    private final OnDeviceActionListener listener;

    public DeviceRecyclerViewAdapter(List<DeviceInfo> items,
                                     OnDeviceActionListener listener) {
        mValues = items;
        this.listener = listener;
    }

    public void setItems(List<DeviceInfo> items) {
        mValues.clear();
        mValues.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentDeviceInfoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mDeviceUuid.setText(mValues.get(position).getUuid());
        holder.mDeviceName.setText(mValues.get(position).getDevName());
        holder.deviceClass.setText(mValues.get(position).getDeviceType());
        holder.mDeviceOnline.setText(mValues.get(position).getOnlineStatus() == null ? OnlineStatus.UNKNOWN.name() : mValues.get(position).getOnlineStatus().name());
        boolean rollerShutter = isRollerShutter(mValues.get(position));
        holder.mDeviceIcon.setImageResource(rollerShutter
                ? R.drawable.ic_roller_shutter : R.drawable.ic_smart_plug);
        holder.mDeviceIcon.setContentDescription(rollerShutter
                ? "Roller shutter" : "Device");
        holder.mDeleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteDevice(holder.mItem);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRenameDevice(holder.mItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    private static boolean isRollerShutter(DeviceInfo device) {
        String type = device.getDeviceType();
        return type != null && ROLLER_SHUTTER_TYPES.contains(type.toLowerCase(Locale.ROOT));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mDeviceUuid;
        public final TextView mDeviceName;
        public final TextView deviceClass;
        public final TextView mDeviceOnline;
        public final ImageView mDeleteButton;
        public final ImageView mDeviceIcon;
        public DeviceInfo mItem;

        public ViewHolder(FragmentDeviceInfoBinding binding) {
            super(binding.getRoot());
            mDeviceUuid = binding.deviceUuid;
            mDeviceName = binding.deviceName;
            deviceClass = binding.deviceClass;
            mDeviceOnline = binding.deviceOnlineStatus;
            mDeleteButton = binding.deviceDeleteButton;
            mDeviceIcon = binding.imageView9;
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + mDeviceName.getText();
        }
    }
}