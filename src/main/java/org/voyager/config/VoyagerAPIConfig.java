package org.voyager.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.voyager.utils.ConstantsUtils;
import java.util.List;

import static org.voyager.utils.ConstantsUtils.QUERY_PARAM;
import static org.voyager.utils.ConstantsUtils.SKIP_ROW_PARAM;
import static org.voyager.utils.ConstantsUtils.LATITUDE_PARAM;
import static org.voyager.utils.ConstantsUtils.LONGITUDE_PARAM;
import static org.voyager.utils.ConstantsUtils.LIMIT_PARAM;

@Component
@ConfigurationProperties(prefix = "voyager-api")
@Setter
@Getter
public class VoyagerAPIConfig {

    String protocol;
    String host;
    Integer port;
    String lookupPath;
    String lookupAttributionPath;
    String townPath;
    String iataPath;
    String nearbyAirportsPath;
    String authToken;
    private HttpEntity<String> httpEntityWithHeaders;

    @PostConstruct
    public void validate() {
        // TODO: add path validators
        ConstantsUtils.validateEnvironVars(List.of(ConstantsUtils.VOYAGER_API_KEY));
        HttpHeaders headers = new HttpHeaders();
        headers.set(ConstantsUtils.AUTH_TOKEN_HEADER_NAME,authToken);
        httpEntityWithHeaders = new HttpEntity<>(headers);
    }

    public HttpEntity<String> getHttpEntity() {
        return httpEntityWithHeaders;
    }

    public String buildLookupURL(String query, int skipRows) {
        return UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(lookupPath)
                .queryParam(QUERY_PARAM,query)
                .queryParam(SKIP_ROW_PARAM,skipRows)
                .toUriString();
    }

    public String buildGetTownsURL() {
        return UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(townPath)
                .toUriString();
    }

    public String buildIataCodesURL() {
        return UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(iataPath)
                .toUriString();
    }

    public String buildNearbyAirportsURL(double latitude,double longitude,int limit) {
        return UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(nearbyAirportsPath)
                .queryParam(LATITUDE_PARAM,latitude)
                .queryParam(LONGITUDE_PARAM,longitude)
                .queryParam(LIMIT_PARAM,limit)
                .toUriString();
    }

    public String buildLookupAttributionURL() {
        return UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(lookupAttributionPath)
                .toUriString();
    }
}
