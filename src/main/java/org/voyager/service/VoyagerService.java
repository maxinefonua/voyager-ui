package org.voyager.service;

import org.voyager.model.Airline;
import org.voyager.model.ResultDetails;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.flight.Flight;
import org.voyager.model.location.*;
import org.voyager.model.response.SearchResult;
import org.voyager.model.result.LookupAttribution;
import org.voyager.model.result.ResultSearch;
import org.voyager.model.result.ResultSearchFull;
import org.voyager.model.route.PathAirline;
import org.voyager.model.route.PathResponse;
import org.voyager.model.route.Route;
import org.voyager.service.impl.CountryServiceAPI;
import org.voyager.service.impl.LocationServiceAPI;

import java.util.List;
import java.util.Map;

public interface VoyagerService {
    SearchResult<ResultSearch> lookup(String query, int skipRows, int limit);
    SearchResult<ResultDetails> lookupWithDetails(String query, int skipRows, int limit);
    LookupAttribution lookupAttribution();
    List<Airport> nearbyAirports(double latitude, double longitude, int limit,AirportType type);
    List<Airport> nearbyAirports(double latitude, double longitude, int limit,Airline airline);
    List<Airport> nearbyAirportsAllActiveAirlines(double latitude, double longitude, int limit);
    List<Location> getLocations();
    List<Location> getLocations(Status status);
    Location getLocation(Integer id);
    Location getLocation(Source source, String sourceId);
    Location patchLocation(Integer id, LocationPatch locationPatch);
    Location addLocation(LocationForm locationForm);
    List<Airport> airports(AirportType type, Airline airline);
    List<Airport> airports(AirportType type);
    List<Airport> airports(List<AirportType> type);
    List<Airport> airports(Airline airline);
    List<Airport> airports();
    List<Flight> getFlights(Integer routeId,boolean isActive);
    List<Flight> getFlights(Integer routeId,boolean isActive,Airline airline);
    Boolean isValidIataCode(String airportCode);
    Boolean isDeltaIataCode(String airportCode);
    Airport getAirport(String iata);
    PathResponse<PathAirline> getPath(List<String> originList, List<String> destinationList);
    PathResponse<PathAirline> getPath(List<String> originList, List<String> destinationList, List<String> excludeAirportList, List<Integer> excludeRouteIdList, Airline airline);
    PathResponse<PathAirline> getPath(List<String> originList, List<String> destinationList, List<String> excludeAirportList, List<Integer> excludeRouteIdList);
    Route getRoute(Integer id);
    ResultSearchFull getResultSearchFull(String sourceId);

    List<Airline> getAirlines(List<String> iataList);

    CountryServiceAPI getCountryServiceAPI();
    LocationServiceAPI getLocationServiceAPI();
}
