package org.voyager.service.impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.config.VoyagerAPIConfig;
import org.voyager.model.Airline;
import org.voyager.model.AirportDisplay;
import org.voyager.model.AirportType;
import org.voyager.model.TownDisplay;
import org.voyager.model.location.LocationDisplay;
import org.voyager.model.response.VoyagerListResponse;
import org.voyager.model.response.VoyagerResponseAPI;
import org.voyager.model.result.LookupAttribution;
import org.voyager.model.result.ResultSearch;
import org.voyager.service.VoyagerAPI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class VoyagerAPIService implements VoyagerAPI {
    @Autowired
    VoyagerAPIConfig voyagerAPIConfig;
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final Logger LOGGER = LoggerFactory.getLogger(VoyagerAPIService.class);

    public VoyagerListResponse<ResultSearch> lookup(String query, int skipRows) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String lookupURL = voyagerAPIConfig.buildLookupURL(encodedQuery,skipRows);
        LOGGER.info(String.format("full lookup URL: %s",lookupURL));
        ResponseEntity<VoyagerListResponse<ResultSearch>> searchResponse = restTemplate.exchange(
                lookupURL,
                HttpMethod.GET,
                voyagerAPIConfig.getHttpEntity(),
                new ParameterizedTypeReference<VoyagerListResponse<ResultSearch>>() {});
        validateVoyagerResponse(searchResponse,lookupURL);
        return searchResponse.getBody();
    }

    @Override
    public LookupAttribution lookupAttribution() {
        String attributionURL = voyagerAPIConfig.buildLookupAttributionURL();
        LOGGER.info(String.format("Fetching lookup attribution from URL: %s",attributionURL));
        ResponseEntity<LookupAttribution> attributionResponse = restTemplate.exchange(
                attributionURL,
                HttpMethod.GET,
                voyagerAPIConfig.getHttpEntity(),
                LookupAttribution.class);
        validateVoyagerResponse(attributionResponse,attributionURL);
        return attributionResponse.getBody();
    }

    @Override
    public VoyagerResponseAPI<TownDisplay> towns() {
        String townsURL = voyagerAPIConfig.buildGetTownsURL();
        LOGGER.debug("full towns URL: " + townsURL);
        ResponseEntity<List<TownDisplay>> townsResponse = restTemplate
                .exchange(townsURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        new ParameterizedTypeReference<List<TownDisplay>>() {});
        validateVoyagerResponse(townsResponse,townsURL);
        List<TownDisplay> towns = townsResponse.getBody();
        return new VoyagerResponseAPI<>(towns.size(),towns);
    }

    @Override
    public List<AirportDisplay> nearbyAirports(double latitude, double longitude, int limit, Optional<AirportType> type, Optional<Airline> airline) {
        String nearbyAirportsURL = voyagerAPIConfig.buildNearbyAirportsURL(latitude,longitude,limit,type,airline);
        LOGGER.debug("full nearbyAirports URL: " + nearbyAirportsURL);
        ResponseEntity<List<AirportDisplay>> airportsResponse = restTemplate
                .exchange(nearbyAirportsURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        new ParameterizedTypeReference<List<AirportDisplay>>() {});
        validateVoyagerResponse(airportsResponse,nearbyAirportsURL);
        return airportsResponse.getBody();
    }

    @Override
    public List<LocationDisplay> getLocations() {
        String locationsURL = voyagerAPIConfig.buildGetLocationsURL();
        LOGGER.debug("full locations URL: " + locationsURL);
        ResponseEntity<List<LocationDisplay>> airportsResponse = restTemplate
                .exchange(locationsURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        new ParameterizedTypeReference<List<LocationDisplay>>() {});
        validateVoyagerResponse(airportsResponse,locationsURL);
        return airportsResponse.getBody();
    }
}
