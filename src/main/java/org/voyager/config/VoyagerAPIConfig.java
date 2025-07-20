package org.voyager.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.voyager.model.Airline;
import org.voyager.model.airport.AirportType;
import org.voyager.utils.ConstantsUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.voyager.utils.ConstantsUtils.*;

@Component
@ConfigurationProperties(prefix = "voyager-api")
@Setter
@Getter
public class VoyagerAPIConfig {
    String protocol;
    String host;
    Integer port;
    Integer maxThreads;
    String authToken;

    public VoyagerConfig getVoyagerConfig() {
        return new VoyagerConfig(Protocol.valueOf(this.getProtocol().toUpperCase()),this.getHost(),this.getPort(),this.getMaxThreads(),this.getAuthToken());
    }
}
