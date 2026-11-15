package org.localpairer.merosslib;

import org.localpairer.merosslib.model.Cipher;
import org.localpairer.merosslib.model.Encryption;
import org.localpairer.merosslib.model.OnlineStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Utils {
    public static Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Cipher.class, new Cipher.TypeDeserializer() );
        gsonBuilder.registerTypeAdapter(Encryption.class, new Encryption.TypeDeserializer() );
        gsonBuilder.registerTypeAdapter(OnlineStatus.class, new OnlineStatus.TypeDeserializer() );
        Gson gson = gsonBuilder.create();
        return gson;
    }
}
