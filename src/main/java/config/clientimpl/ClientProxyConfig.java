package config.clientimpl;

import config.EeProxyConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ClientProxyConfig extends EeProxyConfig {
    private List<String> targetHostPatterns;
}
