package org.localpairer.merosslib.model.protocol.payloads;

import org.localpairer.merosslib.model.protocol.MessagePayload;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Getter;

@Getter
public class GetConfigWifiListPayload extends MessagePayload {
    @SerializedName("wifiList")
    private List<GetConfigWifiListEntry> wifiList;
}
