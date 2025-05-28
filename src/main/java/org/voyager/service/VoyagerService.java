package org.voyager.service;

import org.voyager.model.Airline;
import org.voyager.model.ResultDetails;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.location.*;
import org.voyager.model.response.SearchResult;
import org.voyager.model.result.LookupAttribution;
import org.voyager.model.result.ResultSearch;
import org.voyager.model.route.Path;

import java.util.List;
import java.util.Set;

public interface VoyagerService {
    SearchResult<ResultSearch> lookup(String query, int skipRows, int limit);
    SearchResult<ResultDetails> lookupWithDetails(String query, int skipRows, int limit);
    LookupAttribution lookupAttribution();
    List<Airport> nearbyAirports(double latitude, double longitude, int limit,AirportType type);
    List<Airport> nearbyAirports(double latitude, double longitude, int limit,Airline airline);
    List<Location> getLocations();
    List<Location> getLocations(Status status);
    Location getLocation(Integer id);
    Location getLocation(Source source, String sourceId);
    Location patchLocation(Integer id, LocationPatch locationPatch);
    Location addLocation(LocationForm locationForm);
    List<Airport> airports(AirportType type, Airline airline);
    List<Airport> airports(AirportType type);
    List<Airport> airports(Airline airline);
    List<Airport> airports();
    Boolean isValidIataCode(String airportCode);
    Boolean isDeltaIataCode(String airportCode);
    Airport getAirport(String iata);
    Path getPath(String origin, String destination);
    Path getPath(String origin, String destination, Set<String> exclusions);
}
