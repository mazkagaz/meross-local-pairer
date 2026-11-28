package org.localpairer.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CRC32;

/**
 * Minimal Meross BLE transport, implemented from the protocol used by the
 * device's GATT service (A00A / B002 write / B003 notify).
 */
@SuppressLint("MissingPermission")
public class MerossBleClient {
    public interface Callback {
        void onDeviceFound(BluetoothDevice device, String name);

        void onReady();

        void onPacket(JSONObject packet);

        void onError(String message);

        void onDisconnected();
    }

    private static final String TAG = "MerossBle";
    public static final UUID SERVICE_UUID =
            UUID.fromString("0000A00A-0000-1000-8000-00805F9B34FB");
    public static final UUID WRITE_UUID =
            UUID.fromString("0000B002-0000-1000-8000-00805F9B34FB");
    public static final UUID NOTIFY_UUID =
            UUID.fromString("0000B003-0000-1000-8000-00805F9B34FB");
    private static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private final Callback callback;
    private volatile boolean closed;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService writer = Executors.newSingleThreadExecutor();

    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic notifyCharacteristic;
    private int maxPacketSize = 20;
    private byte[] receiveBuffer;
    private int receiveIndex;
    private boolean scanRunning;

    public MerossBleClient(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
        BluetoothManager manager =
                (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager != null ? manager.getAdapter() : null;
    }

    public boolean isSupported() {
        return adapter != null && context.getPackageManager()
                .hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public boolean isEnabled() {
        return adapter != null && adapter.isEnabled();
    }

    @SuppressLint("MissingPermission")
    public void startScan() {
        if (closed) {
            return;
        }
        if (adapter == null || !adapter.isEnabled()) {
            callback.onError("Bluetooth is disabled");
            return;
        }
        stopScan();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        scanRunning = true;
        try {
            adapter.getBluetoothLeScanner().startScan(
                    Collections.singletonList(filter), settings, scanCallback);
        } catch (Exception e) {
            scanRunning = false;
            callback.onError("BLE scan failed: " + e.getMessage());
            return;
        }
        handler.postDelayed(this::stopScan, 12000);
    }

    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (adapter == null || !scanRunning) {
            return;
        }
        scanRunning = false;
        try {
            adapter.getBluetoothLeScanner().stopScan(scanCallback);
        } catch (Exception ignored) {
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result == null || result.getDevice() == null) {
                return;
            }
            BluetoothDevice device = result.getDevice();
            String name = result.getScanRecord() != null
                    ? result.getScanRecord().getDeviceName() : null;
            callback.onDeviceFound(device, name);
        }

        @Override
        public void onScanFailed(int errorCode) {
            scanRunning = false;
            callback.onError("BLE scan failed with code " + errorCode);
        }
    };

    @SuppressLint("MissingPermission")
    public void connect(String address) {
        if (closed) {
            return;
        }
        stopScan();
        closeGatt();
        try {
            BluetoothDevice device = adapter.getRemoteDevice(address);
            gatt = device.connectGatt(context, false, gattCallback);
        } catch (Exception e) {
            callback.onError("BLE connect failed: " + e.getMessage());
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                g.requestMtu(203);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                closeGatt();
                callback.onDisconnected();
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt g, int mtu, int status) {
            maxPacketSize = Math.max(20, mtu - 3);
            g.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            BluetoothGattService service = g.getService(SERVICE_UUID);
            if (service == null) {
                callback.onError("Meross BLE service A00A not found");
                return;
            }
            writeCharacteristic = service.getCharacteristic(WRITE_UUID);
            notifyCharacteristic = service.getCharacteristic(NOTIFY_UUID);
            if (writeCharacteristic == null || notifyCharacteristic == null) {
                callback.onError("Meross BLE characteristics B002/B003 not found");
                return;
            }
            g.setCharacteristicNotification(notifyCharacteristic, true);
            BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(CCCD_UUID);
            if (descriptor == null) {
                callback.onReady();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                g.writeDescriptor(descriptor);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor descriptor, int status) {
            if (descriptor.getUuid().equals(CCCD_UUID)) {
                callback.onReady();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic characteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                handleNotification(characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic characteristic,
                                            byte[] value) {
            handleNotification(value);
        }
    };

    private void handleNotification(byte[] value) {
        if (value == null || value.length == 0) {
            return;
        }
        try {
            if (value.length >= 4 && (value[0] & 0xFF) == 0x55 && (value[1] & 0xFF) == 0xAA) {
                int length = ((value[2] & 0xFF) << 8) | (value[3] & 0xFF);
                receiveBuffer = new byte[length + 4];
                boolean complete = endsWithEndMarker(value);
                int copyLength = value.length - 4 - (complete ? 2 : 0);
                receiveIndex = Math.max(0, Math.min(copyLength, receiveBuffer.length));
                System.arraycopy(value, 4, receiveBuffer, 0, receiveIndex);
                if (complete) {
                    finishPacket();
                }
            } else if (endsWithEndMarker(value)) {
                if (receiveBuffer == null) {
                    return;
                }
                int copy = value.length - 2;
                if (receiveIndex + copy <= receiveBuffer.length) {
                    System.arraycopy(value, 0, receiveBuffer, receiveIndex, copy);
                    receiveIndex += copy;
                }
                finishPacket();
            } else if (receiveBuffer != null) {
                if (receiveIndex + value.length <= receiveBuffer.length) {
                    System.arraycopy(value, 0, receiveBuffer, receiveIndex, value.length);
                    receiveIndex += value.length;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "BLE packet assembly failed", e);
            callback.onError("BLE packet assembly failed: " + e.getMessage());
        }
    }

    private boolean endsWithEndMarker(byte[] value) {
        return value.length >= 2
                && (value[value.length - 2] & 0xFF) == 0xAA
                && (value[value.length - 1] & 0xFF) == 0x55;
    }

    private void finishPacket() {
        if (receiveBuffer == null || receiveIndex < 4) {
            receiveBuffer = null;
            receiveIndex = 0;
            return;
        }
        int jsonLength = receiveIndex - 4;
        byte[] jsonBytes = new byte[jsonLength];
        System.arraycopy(receiveBuffer, 0, jsonBytes, 0, jsonLength);
        receiveBuffer = null;
        receiveIndex = 0;
        try {
            JSONObject packet = new JSONObject(new String(jsonBytes, StandardCharsets.UTF_8));
            callback.onPacket(packet);
        } catch (Exception e) {
            callback.onError("Invalid BLE JSON: " + e.getMessage());
        }
    }

    public void send(JSONObject packet) {
        if (closed) {
            return;
        }
        byte[] framed = frame(packet);
        try {
            writer.execute(() -> {
            List<byte[]> chunks = new ArrayList<>();
            for (int offset = 0; offset < framed.length; offset += maxPacketSize) {
                int length = Math.min(maxPacketSize, framed.length - offset);
                byte[] chunk = new byte[length];
                System.arraycopy(framed, offset, chunk, 0, length);
                chunks.add(chunk);
            }
            for (int i = 0; i < chunks.size(); i++) {
                if (!write(chunks.get(i))) {
                    return;
                }
                if (i < chunks.size() - 1) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            callback.onError("BLE transport is closed");
        }
    }

    private boolean write(byte[] data) {
        BluetoothGattCharacteristic characteristic = writeCharacteristic;
        BluetoothGatt current = gatt;
        if (characteristic == null || current == null) {
            callback.onError("BLE write characteristic is not ready");
            return false;
        }
        boolean ok;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ok = current.writeCharacteristic(characteristic, data,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                    == BluetoothStatusCodes.SUCCESS;
        } else {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            characteristic.setValue(data);
            ok = current.writeCharacteristic(characteristic);
        }
        if (!ok) {
            callback.onError("BLE write failed");
        }
        return ok;
    }

    public static byte[] frame(JSONObject packet) {
        byte[] json = packet.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[json.length + 10];
        out[0] = 0x55;
        out[1] = (byte) 0xAA;
        out[2] = (byte) ((json.length >> 8) & 0xFF);
        out[3] = (byte) (json.length & 0xFF);
        System.arraycopy(json, 0, out, 4, json.length);
        int crc = crc32(json);
        int offset = json.length + 4;
        out[offset] = (byte) ((crc >> 24) & 0xFF);
        out[offset + 1] = (byte) ((crc >> 16) & 0xFF);
        out[offset + 2] = (byte) ((crc >> 8) & 0xFF);
        out[offset + 3] = (byte) (crc & 0xFF);
        out[offset + 4] = (byte) 0xAA;
        out[offset + 5] = 0x55;
        return out;
    }

    private static int crc32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return (int) crc.getValue();
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        closed = true;
        handler.removeCallbacksAndMessages(null);
        stopScan();
        if (gatt != null) {
            try {
                gatt.disconnect();
            } catch (Exception ignored) {
            }
        }
        closeGatt();
        writer.shutdownNow();
    }

    @SuppressLint("MissingPermission")
    private void closeGatt() {
        if (gatt != null) {
            try {
                gatt.close();
            } catch (Exception ignored) {
            }
            gatt = null;
        }
        writeCharacteristic = null;
        notifyCharacteristic = null;
        receiveBuffer = null;
        receiveIndex = 0;
    }
}
