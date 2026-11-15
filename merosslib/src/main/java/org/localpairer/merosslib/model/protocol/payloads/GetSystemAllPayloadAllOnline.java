package org.localpairer.merosslib.model.protocol.payloads;

import org.localpairer.merosslib.model.OnlineStatus;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class GetSystemAllPayloadAllOnline {
    @SerializedName("online")
    private OnlineStatus status;
}
