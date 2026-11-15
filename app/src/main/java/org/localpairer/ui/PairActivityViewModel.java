package org.localpairer.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.localpairer.model.MqttConfiguration;
import org.localpairer.model.MerossDeviceAp;
import org.localpairer.model.WifiConfiguration;
import org.localpairer.model.WifiLocationStatus;
import org.localpairer.merosslib.model.http.ApiCredentials;
import org.localpairer.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import org.localpairer.merosslib.model.protocol.MessageGetSystemAllResponse;

public class PairActivityViewModel extends ViewModel {
    // Wrapper around the Meross AP
    private MutableLiveData<org.localpairer.merosslib.MerossDeviceAp> apDevice;
    private MutableLiveData<MerossDeviceAp> merossDeviceWifiAp;
    private MutableLiveData<WifiConfiguration> localWifiNetwork;
    private MutableLiveData<MessageGetConfigWifiListResponse> deviceAvailableWifis;
    private MutableLiveData<MqttConfiguration> targetMqttConfig;
    private MutableLiveData<MessageGetSystemAllResponse> deviceInfo;
    private MutableLiveData<WifiLocationStatus> wifiLocationStatus;
    private MutableLiveData<String> overridedKey;
    private MutableLiveData<String> overridedUserId;


    public PairActivityViewModel() {
        apDevice = new MutableLiveData<>(null);
        merossDeviceWifiAp = new MutableLiveData<>(null);
        localWifiNetwork = new MutableLiveData<>(null);
        targetMqttConfig = new MutableLiveData<>(null);
        deviceInfo = new MutableLiveData<>(null);
        deviceAvailableWifis = new MutableLiveData<>(null);
        wifiLocationStatus = new MutableLiveData<>(null);
        overridedKey = new MutableLiveData<>(null);
        overridedUserId = new MutableLiveData<>(null);
    }

    /**
     * Clears every piece of state tied to a pairing attempt. Called before a
     * new device is selected so a failed attempt cannot leak its AP, UUID or
     * broker configuration into the next one.
     */
    public void resetPairingState() {
        apDevice.setValue(null);
        merossDeviceWifiAp.setValue(null);
        localWifiNetwork.setValue(null);
        deviceAvailableWifis.setValue(null);
        targetMqttConfig.setValue(null);
        deviceInfo.setValue(null);
        overridedKey.setValue(null);
        overridedUserId.setValue(null);
    }

    public LiveData<org.localpairer.merosslib.MerossDeviceAp> getDevice() {
        return apDevice;
    }
    public void setApDevice(org.localpairer.merosslib.MerossDeviceAp device) {
        this.apDevice.setValue(device);
    }

    public LiveData<MerossDeviceAp> getMerossPairingAp() {
        return this.merossDeviceWifiAp;
    }
    public void setMerossPairingAp(MerossDeviceAp apInfo) {
        this.merossDeviceWifiAp.setValue(apInfo);
    }

    public LiveData<WifiConfiguration> getMerossConfiguredWifi() {
        return this.localWifiNetwork;
    }
    public void setMerossWifiConfiguration(WifiConfiguration wifiConfiguration) {
        this.localWifiNetwork.setValue(wifiConfiguration);
    }

    public LiveData<MessageGetSystemAllResponse> getDeviceInfo() {
        return this.deviceInfo;
    }
    public void setDeviceInfo(MessageGetSystemAllResponse deviceInfo) {
        this.deviceInfo.setValue(deviceInfo);
    }

    public LiveData<MessageGetConfigWifiListResponse> getDeviceAvailableWifis() {
        return this.deviceAvailableWifis;
    }
    public void setDeviceAvailableWifis(MessageGetConfigWifiListResponse deviceAvailableWifis) {
        this.deviceAvailableWifis.setValue(deviceAvailableWifis);
    }

    public LiveData<MqttConfiguration> getTargetMqttConfig() {
        return this.targetMqttConfig;
    }
    public void setTargetMqttConfig(MqttConfiguration mqttConfig) {
        this.targetMqttConfig.setValue(mqttConfig);
    }

    public LiveData<WifiLocationStatus> getWifiLocationStatus() {
        return this.wifiLocationStatus;
    }
    public void setWifiLocationStatus(WifiLocationStatus status) {
        this.wifiLocationStatus.setValue(status);
    }

    public LiveData<String> getOverridedUserId() {
        return this.overridedUserId;
    }
    public void setOverrideUserId(String overrideUserId) {
        this.overridedUserId.setValue(overrideUserId);
    }

    public LiveData<String> getOverridedKey() {
        return this.overridedKey;
    }
    public void setOverrideKey(String overrideKey) {
        this.overridedKey.setValue(overrideKey);
    }
}