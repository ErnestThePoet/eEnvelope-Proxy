package config.clientimpl;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientConfigManager {
    private static ClientProxyConfig config;

    public static void load() throws IOException {
        ObjectMapper objectMapper=new ObjectMapper();

        String configJsonContent= Files.readString(
                Path.of("./configs/configs_client.json"), StandardCharsets.UTF_8);

        config = objectMapper.readValue(configJsonContent, ClientProxyConfig.class);
    }

    public static Integer getPort(){
        return config.getPort();
    }

    public static boolean isTargetHost(String host){
        return config.getTargetHostPatterns().stream().anyMatch(host::matches);
    }
}
