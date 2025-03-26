package org.voyager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConfigurationProperties(prefix = "voyager-api")
@Setter
@Getter
public class VoyagerAPIConfig {
    private static final String QUERY_KEY = "searchText";
    private static final String START_ROW_KEY = "startRow";
    String protocol;
    String host;
    Integer port;
    String lookupPath;
    String auth;

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
}
