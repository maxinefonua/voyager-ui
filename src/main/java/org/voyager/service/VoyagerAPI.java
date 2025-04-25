package org.voyager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.model.Airline;
import org.voyager.model.AirportDisplay;
import org.voyager.model.AirportType;
import org.voyager.model.TownDisplay;
import org.voyager.model.location.LocationDisplay;
import org.voyager.model.location.LocationForm;
import org.voyager.model.response.VoyagerListResponse;
import org.voyager.model.response.VoyagerResponseAPI;
import org.voyager.model.result.LookupAttribution;
import org.voyager.model.result.ResultSearch;

import java.util.List;
import java.util.Optional;

public interface VoyagerAPI {
    public static final Logger LOGGER = LoggerFactory.getLogger(VoyagerAPI.class);
    public abstract VoyagerListResponse<ResultSearch> lookup(String query, int skipRows);
    public abstract LookupAttribution lookupAttribution();
    public abstract VoyagerResponseAPI<TownDisplay> towns();
    public abstract List<AirportDisplay> nearbyAirports(double latitude, double longitude, int limit, Optional<AirportType> type, Optional<Airline> airline);
    public abstract List<LocationDisplay> getLocations();
    public abstract LocationDisplay addLocation(LocationForm locationForm);
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
