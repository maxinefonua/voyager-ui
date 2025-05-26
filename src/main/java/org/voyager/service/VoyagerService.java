package org.voyager.service;

import org.voyager.model.Airline;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.location.Location;
import org.voyager.model.location.LocationForm;
import org.voyager.model.location.LocationPatch;
import org.voyager.model.response.SearchResult;
import org.voyager.model.result.LookupAttribution;
import org.voyager.model.result.ResultSearch;
import org.voyager.model.route.Path;

import java.util.List;
import java.util.Set;

public interface VoyagerService {
    SearchResult<ResultSearch> lookup(String query, int skipRows, int limit);
    LookupAttribution lookupAttribution();
    List<Airport> nearbyAirports(double latitude, double longitude, int limit,AirportType type);
    List<Airport> nearbyAirports(double latitude, double longitude, int limit,Airline airline);
    List<Location> getLocations();
    Location getLocation(Integer id);
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
