package org.localpairer.merosslib;

import org.localpairer.merosslib.model.protocol.Header;
import org.localpairer.merosslib.model.protocol.Message;
import org.localpairer.merosslib.model.protocol.MessageGetConfigWifiList;
import org.localpairer.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import org.localpairer.merosslib.model.protocol.MessageGetSystemAll;
import org.localpairer.merosslib.model.protocol.MessageGetSystemAllResponse;
import org.localpairer.merosslib.model.protocol.MessageSetConfigKey;
import org.localpairer.merosslib.model.protocol.MessageSetConfigKeyResponse;
import org.localpairer.merosslib.model.protocol.MessageSetConfigWifi;
import org.localpairer.merosslib.model.protocol.MessageSetConfigWifiResponse;
import org.localpairer.merosslib.model.protocol.Method;
import org.localpairer.merosslib.model.protocol.payloads.GetConfigWifiListEntry;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import lombok.Getter;


public class MerossDeviceAp implements Serializable {

    @Getter
    private String ip;
    @Getter
    private String cloudKey;
    private OkHttpClient client;
    private Gson g = Utils.getGson();

    public void setSocketFactory(SocketFactory factory) {
        client.setSocketFactory(factory);
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setCloudKey(String cloudKey) {
        this.cloudKey = cloudKey;
    }

    public MerossDeviceAp(String ip, String cloudKey) {
        this.ip = ip;
        this.cloudKey = cloudKey;
        this.client = new OkHttpClient();
        this.client.setConnectTimeout(40, TimeUnit.SECONDS);
        this.client.setReadTimeout(40, TimeUnit.SECONDS);
    }

    public MerossDeviceAp() {
        this("10.10.10.1", "");
    }

    public MessageGetSystemAllResponse getConfig() throws IOException {
        Message message = MessageGetSystemAll.BuildNew();
        return this.sendMessage(message, MessageGetSystemAllResponse.class);
    }

    public MessageGetConfigWifiListResponse scanWifi() throws IOException {
        Message message = MessageGetConfigWifiList.BuildNew();
        return this.sendMessage(message, MessageGetConfigWifiListResponse.class);
    }

    public MessageSetConfigKeyResponse setConfigKey(String hostname, int port, String key, String userId ) throws IOException {
        Message message = MessageSetConfigKey.BuildNew(hostname, port, key, userId);
        return this.sendMessage(message, MessageSetConfigKeyResponse.class);
    }

    public MessageSetConfigWifiResponse setConfigWifi(GetConfigWifiListEntry wifiConfig, String base64password) throws IOException {
        Message message = MessageSetConfigWifi.BuildNew(wifiConfig.getBase64ssid(), base64password, wifiConfig.getBssid(), wifiConfig.getChannel(), wifiConfig.getCipher(), wifiConfig.getEncryption());
        return this.sendMessage(message, MessageSetConfigWifiResponse.class);
    }

    private <T> T sendMessage(Message message, Class<T> type) throws IOException {
        message.sign(cloudKey); // Signature is not verified by the device in AP mode.

        Request request = new Request.Builder()
                .url("http://" + ip + "/config")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), g.toJson(message).getBytes("utf8")))
                .build();
        Response response = client.newCall(request).execute();

        String body = response.body().string();
        if (response.code() != 200) {
            throw new IOException("Invalid response code (" + response.code() + ") received from Meross Device: " + body);
        }

        T parsed = g.fromJson(body, type);
        if (parsed instanceof Message) {
            Header requestHeader = message.getHeader();
            Header responseHeader = ((Message) parsed).getHeader();
            if (responseHeader == null || responseHeader.getMethod() == null) {
                throw new IOException("Malformed response from Meross Device: " + body);
            }
            if (responseHeader.getMethod() == Method.ERROR) {
                throw new IOException("Device refused " + requestHeader.getNamespace() + ": " + body);
            }
            Method expectedMethod = requestHeader.getMethod() == Method.GET ? Method.GETACK : Method.SETACK;
            if (responseHeader.getMethod() != expectedMethod) {
                throw new IOException("Unexpected method " + responseHeader.getMethod()
                        + " for " + requestHeader.getNamespace() + ": " + body);
            }
            if (responseHeader.getNamespace() != null
                    && !requestHeader.getNamespace().contentEquals(responseHeader.getNamespace())) {
                throw new IOException("Unexpected namespace " + responseHeader.getNamespace()
                        + " for " + requestHeader.getNamespace() + ": " + body);
            }
        }
        return parsed;
    }
}
