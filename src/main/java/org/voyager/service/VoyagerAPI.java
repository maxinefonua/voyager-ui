package org.voyager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.model.Airline;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.location.Location;
import org.voyager.model.location.LocationForm;
import org.voyager.model.response.VoyagerListResponse;
import org.voyager.model.response.VoyagerResponseAPI;
import org.voyager.model.result.LookupAttribution;
import org.voyager.model.result.ResultSearch;
import org.voyager.model.route.Path;

import java.util.List;
import java.util.Optional;

public interface VoyagerAPI {
    public static final Logger LOGGER = LoggerFactory.getLogger(VoyagerAPI.class);
    VoyagerListResponse<ResultSearch> lookup(String query, int skipRows, Optional<Integer> limitOptional);
    LookupAttribution lookupAttribution();
    List<Airport> nearbyAirports(double latitude, double longitude, int limit, Optional<AirportType> type, Optional<Airline> airline);
    List<Location> getLocations();
    Location getLocationById(Integer id);
    Location addLocation(LocationForm locationForm);
    List<Airport> airports(Optional<AirportType> type, Optional<Airline> airline);
    Boolean isValidIataCode(String airportCode);
    Boolean isDeltaIataCode(String airportCode);
    Optional<Airport> getAirportByIata(String iata);
    Path getPath(String origin, String destination);

    public default void validateVoyagerResponse(ResponseEntity responseEntity, String requestURL){
        if (responseEntity.getStatusCode().value() != 200 || responseEntity.getBody() == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Received non-200 status code or null response body from voyager API endpoint: %s",requestURL));
            if (responseEntity.hasBody()) {
                sb.append(String.format("\nResponse: %s",responseEntity.getBody()));
            }
            LOGGER.error(sb.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error occurred fetching data.");
        }
    }

}
