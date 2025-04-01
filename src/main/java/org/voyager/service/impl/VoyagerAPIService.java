package org.voyager.service.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.config.VoyagerAPIConfig;
import org.voyager.model.AirportDisplay;
import org.voyager.model.ResultDisplay;
import org.voyager.model.TownDisplay;
import org.voyager.model.response.SearchResponseGeoNames;
import org.voyager.model.response.VoyagerListResponse;
import org.voyager.model.response.VoyagerResponseAPI;
import org.voyager.model.response.geonames.GeoName;
import org.voyager.service.VoyagerAPI;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
public class VoyagerAPIService implements VoyagerAPI {
    @Autowired
    VoyagerAPIConfig voyagerAPIConfig;
    private RestTemplate restTemplate = new RestTemplate();

    public VoyagerListResponse<ResultDisplay> lookup(String query, int skipRows) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String lookupURL = voyagerAPIConfig.buildLookupURL(encodedQuery,skipRows);
        System.out.println("full lookup URL: " + lookupURL);
        ResponseEntity<VoyagerListResponse<GeoName>> searchResponse = restTemplate.exchange(
                lookupURL,
                HttpMethod.GET,
                voyagerAPIConfig.getHttpEntity(),
                new ParameterizedTypeReference<VoyagerListResponse<GeoName>>() {});
        VoyagerListResponse<GeoName> results = searchResponse.getBody();
        // TODO: update error handling and message
        if (results == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "search results returned null response body");
        return VoyagerListResponse.<ResultDisplay>builder().resultCount(results.getResultCount())
                .results(results.getResults().stream().map(geoName -> ResultDisplay.builder()
                        .name(geoName.getName()).adminName1(geoName.getAdminName1()).countryCode(geoName.getCountryCode())
                        .countryName(geoName.getCountryName()).fclName(geoName.getFclName())
                        .southBound(geoName.getBoundingBox().getSouth()).eastBound(geoName.getBoundingBox().getEast())
                        .northBound(geoName.getBoundingBox().getNorth()).westBound(geoName.getBoundingBox().getWest())
                        .latitude(geoName.getLat()).longitude(geoName.getLng()).build()
                ).toList()).build();
    }

    @Override
    public VoyagerResponseAPI<TownDisplay> towns() {
        String townsURL = voyagerAPIConfig.buildGetTownsURL();
        System.out.println("full towns URL: " + townsURL);
        ResponseEntity<TownDisplay[]> townsResponse = restTemplate
                .exchange(townsURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        TownDisplay[].class);
        // TODO: null check
        TownDisplay[] towns = townsResponse.getBody();
        return new VoyagerResponseAPI<>(towns.length, Arrays.asList(towns));
    }

    @Override
    public List<AirportDisplay> nearbyAirports(double latitude, double longitude, int limit) {
        String nearbyAirportsURL = voyagerAPIConfig.buildNearbyAirportsURL(latitude,longitude,limit);
        System.out.println("full nearbyAirports URL: " + nearbyAirportsURL);
        ResponseEntity<VoyagerListResponse<AirportDisplay>> nearbyAirportsResponse = restTemplate
                .exchange(nearbyAirportsURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        new ParameterizedTypeReference<VoyagerListResponse<AirportDisplay>>() {});
        // TODO: update error handling and message
        VoyagerListResponse<AirportDisplay> responseBody = nearbyAirportsResponse.getBody();
        if (responseBody == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "nearby airports request returned with a null response body");
        return responseBody.getResults();
    }
}
