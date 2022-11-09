package config.serverimpl;

import config.EeProxyConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServerProxyConfig extends EeProxyConfig {
    private String proxyPass;
}
