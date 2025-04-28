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
import org.voyager.model.AirportType;
import org.voyager.utils.ConstantsUtils;

import java.util.List;
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
    String authToken;

    @Value("/search")
    String lookupPath;

    @Value("/search-attribution")
    String lookupAttributionPath;

    @Value("/locations")
    String locationsPath;

    @Value("/towns")
    String townPath;

    @Value("/iata")
    String iataPath;

    @Value("/nearby-airports")
    String nearbyAirportsPath;

    @Value("/airports")
    String airportsPath;

    private HttpEntity<String> httpEntityWithHeaders;
    private UriComponentsBuilder nearbyAirportsURI;

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
                .queryParam(QUERY_PARAM_NAME,query)
                .queryParam(SKIP_ROW_PARAM_NAME,skipRows)
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

    public String buildLocationsURL() {
        return UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(locationsPath)
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

    public String buildNearbyAirportsURL(double latitude, double longitude, int limit, Optional<AirportType> type, Optional<Airline> airline) {
        UriComponentsBuilder nearByURL = UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(nearbyAirportsPath)
                .queryParam(LATITUDE_PARAM_NAME,latitude)
                .queryParam(LONGITUDE_PARAM_NAME,longitude)
                .queryParam(LIMIT_PARAM_NAME,limit);
        type.ifPresent(airportType -> nearByURL.queryParam(TYPE_PARAM_NAME, airportType));
        airline.ifPresent(airportType -> nearByURL.queryParam(AIRLINE_PARAM_NAME, airportType));
        return nearByURL.toUriString();
    }

    public String buildAirportsURL(Optional<AirportType> type, Optional<Airline> airline) {
        UriComponentsBuilder airportsURL = UriComponentsBuilder
                .newInstance().scheme(protocol)
                .host(host)
                .port(port)
                .path(airportsPath);
        type.ifPresent(airportType -> airportsURL.queryParam(TYPE_PARAM_NAME, airportType));
        airline.ifPresent(airportType -> airportsURL.queryParam(AIRLINE_PARAM_NAME, airportType));
        return airportsURL.toUriString();
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
