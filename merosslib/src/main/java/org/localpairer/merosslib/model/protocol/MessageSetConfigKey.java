package org.localpairer.merosslib.model.protocol;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageSetConfigKey extends Message {
    @SerializedName("payload")
    private SetConfigKeyPayload payload;

    public static MessageSetConfigKey BuildNew(String hostname, int port, String key, String userId) {
        return BuildNew(hostname, port, hostname, port, key, userId);
    }

    public static MessageSetConfigKey BuildNew(String hostname, int port,
                                               String secondHostname, int secondPort,
                                               String key, String userId) {
        Header header = Header.BuildNew("Appliance.Config.Key", Method.SET);
        MessageSetConfigKey msg = new MessageSetConfigKey();
        msg.setHeader(header);

        msg.payload = new SetConfigKeyPayload();
        msg.payload.key = new SetConfigKeyPayloadKey();
        msg.payload.key.userId = userId;
        msg.payload.key.key = key;
        msg.payload.key.gateway = new SetConfigKeyPayloadKeyGateway();
        // The official app sends numeric ports and an explicit redirect
        // value (0 for AP provisioning).
        msg.payload.key.gateway.host = hostname;
        msg.payload.key.gateway.port = port;
        msg.payload.key.gateway.secondHost = secondHostname;
        msg.payload.key.gateway.secondPort = secondPort;
        msg.payload.key.gateway.redirect = 0;
        return msg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class SetConfigKeyPayload extends MessagePayload {
        @SerializedName("key")
        private SetConfigKeyPayloadKey key;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class SetConfigKeyPayloadKey {
        @SerializedName("gateway")
        private SetConfigKeyPayloadKeyGateway gateway;

        @SerializedName("key")
        private String key;

        @SerializedName("userId")
        private String userId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class SetConfigKeyPayloadKeyGateway {
        @SerializedName("host")
        private String host;

        @SerializedName("port")
        private Integer port;

        @SerializedName("secondHost")
        private String secondHost;

        @SerializedName("secondPort")
        private Integer secondPort;

        @SerializedName("redirect")
        private Integer redirect;
    }
}
