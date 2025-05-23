package org.voyager.service.impl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.voyager.config.VoyagerAPIConfig;
import org.voyager.model.Airline;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.delta.Delta;
import org.voyager.model.delta.DeltaStatus;
import org.voyager.model.location.Location;
import org.voyager.model.location.LocationForm;
import org.voyager.model.response.VoyagerListResponse;
import org.voyager.model.response.VoyagerResponseAPI;
import org.voyager.model.result.LookupAttribution;
import org.voyager.model.result.ResultSearch;
import org.voyager.model.route.Path;
import org.voyager.model.route.Status;
import org.voyager.service.VoyagerAPI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.voyager.utils.ConstantsUtils.IATA_CODE_REGEX;

@Service
public class VoyagerAPIService implements VoyagerAPI {
    @Autowired
    VoyagerAPIConfig voyagerAPIConfig;
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final Logger LOGGER = LoggerFactory.getLogger(VoyagerAPIService.class);

    public VoyagerListResponse<ResultSearch> lookup(String query, int skipRows,Optional<Integer> limitOptional) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String lookupURL = voyagerAPIConfig.buildLookupURL(encodedQuery,skipRows,limitOptional);
        LOGGER.debug(String.format("full lookup URL: %s",lookupURL));
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
        LOGGER.debug(String.format("Fetching lookup attribution from URL: %s",attributionURL));
        ResponseEntity<LookupAttribution> attributionResponse = restTemplate.exchange(
                attributionURL,
                HttpMethod.GET,
                voyagerAPIConfig.getHttpEntity(),
                LookupAttribution.class);
        validateVoyagerResponse(attributionResponse,attributionURL);
        return attributionResponse.getBody();
    }

    @Override
    public List<Airport> nearbyAirports(double latitude, double longitude, int limit, Optional<AirportType> type, Optional<Airline> airline) {
        String nearbyAirportsURL = voyagerAPIConfig.buildNearbyAirportsURL(latitude,longitude,limit,type,airline);
        LOGGER.debug("full nearbyAirports URL: " + nearbyAirportsURL);
        ResponseEntity<List<Airport>> airportsResponse = restTemplate
                .exchange(nearbyAirportsURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        new ParameterizedTypeReference<List<Airport>>() {});
        validateVoyagerResponse(airportsResponse,nearbyAirportsURL);
        return airportsResponse.getBody();
    }

    @Override
    public List<Location> getLocations() {
        String locationsURL = voyagerAPIConfig.buildLocationsURL();
        LOGGER.debug("full locations URL: " + locationsURL);
        ResponseEntity<List<Location>> locationsResponse = restTemplate
                .exchange(locationsURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        new ParameterizedTypeReference<List<Location>>() {});
        validateVoyagerResponse(locationsResponse,locationsURL);
        return locationsResponse.getBody();
    }

    @Override
    public Location getLocationById(Integer id) {
        String locationByIdURL = voyagerAPIConfig.buildLocationByIdURL(id);
        LOGGER.debug("full location by id URL: " + locationByIdURL);
        ResponseEntity<Location> locationsResponse = restTemplate
                .exchange(locationByIdURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        Location.class);
        validateVoyagerResponse(locationsResponse,locationByIdURL);
        return locationsResponse.getBody();
    }

    @Override
    public Location addLocation(LocationForm locationForm) {
        String locationsURL = voyagerAPIConfig.buildLocationsURL();
        LOGGER.debug("full locations URL: " + locationsURL);
        HttpEntity<LocationForm> requestEntity = new HttpEntity<>(locationForm, voyagerAPIConfig.getHttpEntity().getHeaders());
        ResponseEntity<Location> locationsResponse = restTemplate
                .exchange(locationsURL,
                        HttpMethod.POST,
                        requestEntity,
                        Location.class);
        validateVoyagerResponse(locationsResponse,locationsURL);
        return locationsResponse.getBody();
    }

    @Override
    public List<Airport> airports(Optional<AirportType> type, Optional<Airline> airline) {
        String airportsURL = voyagerAPIConfig.buildAirportsURL(type,airline);
        LOGGER.debug("full airports URL: " + airportsURL);
        ResponseEntity<List<Airport>> airportsResponse = restTemplate
                .exchange(airportsURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        new ParameterizedTypeReference<List<Airport>>() {});
        validateVoyagerResponse(airportsResponse,airportsURL);
        return airportsResponse.getBody();
    }

    @Override
    public Boolean isValidIataCode(String iata) {
        if (StringUtils.isEmpty(iata) || !iata.matches(IATA_CODE_REGEX)) return false;
        return getAirportByIata(iata).isPresent();
    }

    @Override
    public Boolean isDeltaIataCode(String iata) {
        if (StringUtils.isEmpty(iata) || !iata.matches(IATA_CODE_REGEX)) return false;
        return detaAirportExists(iata);
    }

    private Boolean detaAirportExists(String iata) {
        String deltaWithIataUrl = voyagerAPIConfig.buildDeltaWithIataUrl(iata);
        LOGGER.debug("full delta URL: " + deltaWithIataUrl);
        try {
            ResponseEntity<Delta> deltaResponse = restTemplate
                    .exchange(deltaWithIataUrl,
                            HttpMethod.GET,
                            voyagerAPIConfig.getHttpEntity(),
                            Delta.class);
            if (deltaResponse.getStatusCode().value() != 200) return false;
            Delta result = deltaResponse.getBody();
            if (result == null) return false;
            return !result.getStatus().equals(DeltaStatus.TERMINATED);
        } catch (Exception e) {
            LOGGER.info(String.format("Exception thrown when checking if Delta exists for iata %s. Error = %s",iata,e.getMessage()),e);
            return false;
        }
    }

    @Override
    public Optional<Airport> getAirportByIata(String iata) {
        String airportByIataURL = voyagerAPIConfig.buildAirportByIataURL(iata);
        LOGGER.debug("full airport by iata URL: " + airportByIataURL);
        ResponseEntity<Airport> airportResponse = restTemplate
                .exchange(airportByIataURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        Airport.class);
        if (!airportResponse.getStatusCode().is2xxSuccessful()) return Optional.empty();
        assert airportResponse.getBody() != null;
        return Optional.of(airportResponse.getBody());
    }

    @Override
    public Path getPath(String originIata, String destinationIata) {
        String pathURL = voyagerAPIConfig.buildPathURL(originIata,destinationIata);
        LOGGER.debug("full airports URL: " + pathURL);
        ResponseEntity<Path> airportsResponse = restTemplate
                .exchange(pathURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        Path.class);
        validateVoyagerResponse(airportsResponse,pathURL);
        return airportsResponse.getBody();
    }
}
