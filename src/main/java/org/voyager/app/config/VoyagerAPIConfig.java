package org.voyager.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.voyager.sdk.config.Protocol;
import org.voyager.sdk.config.VoyagerConfig;

@Component
@ConfigurationProperties(prefix = "voyager-api")
@Setter @Getter
public class VoyagerAPIConfig {
    String protocol;
    String host;
    Integer port;
    Integer maxThreads;
    String authToken;

    public VoyagerConfig getVoyagerConfig() {
        return new VoyagerConfig(Protocol.valueOf(this.getProtocol().toUpperCase()),this.getHost(),
                this.getPort(),this.getAuthToken());
    }
}
