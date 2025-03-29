package org.voyager.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.voyager.utls.ConstantsUtil;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "voyager-api")
@Setter
@Getter
public class VoyagerAPIConfig {
    // TODO: move to commons
    private static final String QUERY_KEY = "searchText";
    private static final String START_ROW_KEY = "startRow";
    private static final String LATITUDE_KEY = "latitude";
    private static final String LONGITUDE_KEY = "longitude";
    private static final String LIMIT_KEY = "limit";
    String protocol;
    String host;
    Integer port;
    String lookupPath;
    String townPath;
    String iataPath;
    String nearbyAirportsPath;
    String authToken;
    private HttpEntity<String> httpEntityWithHeaders;

    @PostConstruct
    public void validate() {
        // TODO: add path validators
        ConstantsUtil.validateEnvironVars(List.of(ConstantsUtil.VOYAGER_API_KEY));
        HttpHeaders headers = new HttpHeaders();
        headers.set(ConstantsUtil.AUTH_TOKEN_HEADER_NAME,authToken);
        httpEntityWithHeaders = new HttpEntity<>(headers);
    }

    public HttpEntity<String> getHttpEntity() {
        return httpEntityWithHeaders;
    }

    public String buildLookupURL(String query, int skipRows) {
        String searchURL = UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(lookupPath)
                .queryParam(QUERY_KEY,query)
                .queryParam(START_ROW_KEY,skipRows)
                .toUriString();
        return searchURL;
    }

    public String buildGetTownsURL() {
        String getTownsURL = UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(townPath)
                .toUriString();
        return getTownsURL;
    }

    public String buildIataCodesURL() {
        String getIataCodes = UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(iataPath)
                .toUriString();
        return getIataCodes;
    }

    public String buildNearbyAirportsURL(double latitude,double longitude,int limit) {
        String getNearbyAiports = UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(nearbyAirportsPath)
                .queryParam(LATITUDE_KEY,latitude)
                .queryParam(LONGITUDE_KEY,longitude)
                .queryParam(LIMIT_KEY,limit)
                .toUriString();
        return getNearbyAiports;
    }
}
