package org.voyager.service.impl;

import io.vavr.control.Either;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.config.Protocol;
import org.voyager.config.VoyagerAPIConfig;
import org.voyager.config.VoyagerConfig;
import org.voyager.error.HttpStatus;
import org.voyager.error.ServiceError;
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
import org.voyager.service.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.voyager.utils.ConstantsUtils.IATA_CODE_REGEX;

@Service
public class VoyagerServiceImpl implements VoyagerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoyagerServiceImpl.class);

    @Autowired
    VoyagerAPIConfig voyagerAPIConfig;

    private AirportService airportService;
    private RouteService routeService;
    private PathService pathService;
    private SearchService searchService;
    private LocationService locationService;
    private FlightService flightService;

    private List<Airport> allAirports;
    private List<Airport> deltaAirports;
    private Map<String,Airport> airportMap;

    @PostConstruct
    public void init() {
        Voyager voyager = new Voyager(voyagerAPIConfig.getVoyagerConfig());
        this.airportService = voyager.getAirportService();
        this.routeService = voyager.getRouteService();
        this.searchService = voyager.getSearchService();
        this.locationService = voyager.getLocationService();
        this.flightService = voyager.getFlightService();
        this.pathService = voyager.getPathService();

        this.allAirports = fetchAirports();
        this.deltaAirports = fetchAirports(Airline.DELTA);
        this.airportMap = new HashMap<>();
        allAirports.forEach(airport -> airportMap.put(airport.getIata(),airport));
    }

    private Protocol resolveProtocol(String value) {
        Optional<Protocol> optional = Arrays.stream(Protocol.values()).filter(protocol ->
            protocol.getValue().equals(value)).findFirst();
        if (optional.isEmpty()) throw new ResponseStatusException(HttpStatusCode.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.getCode()));
        else return optional.get();
    }

    @Override
    public SearchResult<ResultSearch> lookup(String query, int skipRows, int limit) {
        return fetchSearchResults(query,skipRows,limit);
    }

    @Override
    public SearchResult<ResultDetails> lookupWithDetails(String query, int skipRows, int limit) {
        return fetchSearchResultsWithDetails(query,skipRows,limit);
    }

    @Override
    public LookupAttribution lookupAttribution() {
        return fetchAttribution();
    }

    @Override
    public List<Airport> nearbyAirports(double latitude, double longitude, int limit, AirportType type) {
        return fetchNearbyAirports(latitude,longitude,limit,type);
    }

    @Override
    public List<Airport> nearbyAirports(double latitude, double longitude, int limit, Airline airline) {
        return fetchNearbyAirports(latitude,longitude,limit,airline);
    }

    @Override
    public List<Airport> nearbyAirportsAllActiveAirlines(double latitude, double longitude, int limit) {
        List<Airline> airlines = Arrays.asList(Airline.values());
        return fetchNearbyAirports(latitude,longitude,limit,airlines);
    }

    @Override
    public List<Location> getLocations() {
        return fetchLocations();
    }

    @Override
    public List<Location> getLocations(Status status) {
        return fetchLocations(status);
    }

    @Override
    public Location getLocation(Integer id) {
        return fetchLocation(id);
    }

    @Override
    public Location getLocation(Source source, String sourceId) {
        return fetchLocation(source,sourceId);
    }

    @Override
    public Location patchLocation(Integer id, LocationPatch locationPatch) {
        return fetchPatchedLocation(id,locationPatch);
    }

    @Override
    public Location addLocation(LocationForm locationForm) {
        return createLocation(locationForm);
    }

    @Override
    public List<Airport> airports(@NonNull AirportType type, @NonNull Airline airline) {
        if (airline.equals(Airline.DELTA))
            return deltaAirports.stream().filter(airport -> airport.getType().equals(type)).toList();
        return fetchAirports(type,airline);
    }

    @Override
    public List<Airport> airports(@NonNull AirportType type) {
        return allAirports.stream().filter(airport -> airport.getType().equals(type)).toList();
    }

    @Override
    public List<Airport> airports(List<AirportType> typeList) {
        return allAirports.stream().filter(airport -> typeList.contains(airport.getType())).toList();
    }

    @Override
    public List<Airport> airports(@NonNull Airline airline) {
        if (airline.equals(Airline.DELTA)) return deltaAirports;
        return fetchAirports(airline);
    }

    @Override
    public List<Airport> airports() {
        return allAirports;
    }

    @Override
    public List<Flight> getFlights(Integer routeId,boolean isActive) {
        return fetchFlights(routeId,isActive);
    }

    @Override
    public List<Flight> getFlights(Integer routeId, boolean isActive, Airline airline) {
        return fetchFlights(routeId,isActive,airline);
    }

    @Override
    public Boolean isValidIataCode(String airportCode) {
        if (StringUtils.isBlank(airportCode) || !airportCode.matches(IATA_CODE_REGEX)) return false;
        return allAirports.stream().map(Airport::getIata).collect(Collectors.toSet()).contains(airportCode);
    }

    @Override
    public Boolean isDeltaIataCode(String airportCode) {
        if (StringUtils.isBlank(airportCode) || !airportCode.matches(IATA_CODE_REGEX)) return false;
        return deltaAirports.stream().map(Airport::getIata).collect(Collectors.toSet()).contains(airportCode);
    }

    @Override
    public Airport getAirport(String iata) {
        return airportMap.get(iata);
    }

    @Override
    public PathResponse<PathAirline> getPath(List<String> originList, List<String> destinationList) {
        return fetchPath(originList,destinationList);
    }

    @Override
    public PathResponse<PathAirline> getPath(List<String> originList, List<String> destinationList, List<String> excludeAirportList, List<Integer> excludeRouteIdList, Airline airline) {
        return fetchPath(originList,destinationList, excludeAirportList,excludeRouteIdList,airline);
    }

    @Override
    public PathResponse<PathAirline> getPath(List<String> originList, List<String> destinationList, List<String> excludeAirportList, List<Integer> excludeRouteIdList) {
        return fetchPath(originList,destinationList, excludeAirportList,excludeRouteIdList);
    }

    @Override
    public Route getRoute(Integer id) {
        return fetchRoute(id);
    }

    @Override
    public ResultSearchFull getResultSearchFull(String sourceId) {
        return fetchResultSearchFull(sourceId);
    }

    @Override
    public List<Airline> getAirlines(List<String> iataList) {
        Either<ServiceError,List<Airline>> either = airportService.getAirlines(iataList);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Flight> fetchFlights(Integer routeId, boolean isActive) {
        Either<ServiceError,List<Flight>> either = flightService.getFlights(routeId,isActive);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Flight> fetchFlights(Integer routeId, boolean isActive, Airline airline) {
        Either<ServiceError,List<Flight>> either = flightService.getFlights(routeId,isActive,airline);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private Location createLocation(LocationForm locationForm) {
        Either<ServiceError, Location> either = locationService.createLocation(locationForm);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private Location fetchPatchedLocation(Integer id, LocationPatch locationPatch) {
        Either<ServiceError, Location> either = locationService.patchLocation(id,locationPatch);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private Location fetchLocation(Integer id) {
        Either<ServiceError, Location> either = locationService.getLocation(id);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private Location fetchLocation(Source source, String sourceId) {
        Either<ServiceError, List<Location>> either = locationService.getLocations(source,sourceId);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        List<Location> matches = either.get();
        if (matches.size() > 1) {
            // TODO: handle exception correctly
            String message = String.format("Multiple locations returned for source '%s' and sourceId '%s'. Alerting not yet implemented",
                    source,sourceId);
            LOGGER.error(message);
            throw new ResponseStatusException(HttpStatusCode.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.getCode()),
                    message);
        }
        if (matches.isEmpty()) {
            // TODO: handle exception correctly
            String message = String.format("No locations returned for source '%s' and sourceId '%s'. Alerting not yet implemented",
                    source,sourceId);
            LOGGER.error(message);
            throw new ResponseStatusException(HttpStatusCode.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.getCode()),
                    message);
        }
        return matches.get(0);
    }

    private List<Location> fetchLocations() {
        Either<ServiceError, List<Location>> either = locationService.getLocations();
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Location> fetchLocations(Status status) {
        Either<ServiceError, List<Location>> either = locationService.getLocations(status);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private LookupAttribution fetchAttribution() {
        Either<ServiceError,LookupAttribution> either = searchService.attribution();
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private SearchResult<ResultSearch> fetchSearchResults(String query, int skipRows, int limit) {
        Either<ServiceError,SearchResult<ResultSearch>> either = searchService.search(query,skipRows,limit);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private SearchResult<ResultDetails> fetchSearchResultsWithDetails(String query, int skipRows, int limit) {
        Either<ServiceError,SearchResult<ResultSearch>> either = searchService.search(query,skipRows,limit);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        SearchResult<ResultSearch> results = either.get();
        List<ResultDetails> resultDetailsList = results.getResults().stream()
                .map(this::fetchResultDetails).toList();
        return SearchResult.<ResultDetails>builder().results(resultDetailsList)
                .resultCount(results.getResultCount()).build();
    }

    private ResultDetails fetchResultDetails(ResultSearch resultSearch) {
        ResultDetails resultDetail = ResultDetails.builder().resultSearch(resultSearch).build();
        if (!resultSearch.getStatus().equals(Status.NEW)) {
            Location location = fetchLocation(resultSearch.getSource(), resultSearch.getSourceId());
            resultDetail.setLocation(location);
            List<Airport> airports = new ArrayList<>();
            location.getAirports().forEach(iata -> airports.add(getAirport(iata)));
            resultDetail.setAirportList(airports);
        }
        return resultDetail;
    }

    private PathResponse<PathAirline> fetchPath(List<String> originList, List<String> destinationList, List<String> excludeAirportList, List<Integer> excludeRouteIdList) {
        Either<ServiceError,PathResponse<PathAirline>> either = pathService.getPathAirlineList(originList,destinationList,excludeAirportList,excludeRouteIdList);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private PathResponse<PathAirline> fetchPath(List<String> originList, List<String> destinationList, List<String> excludeAirportList, List<Integer> excludeRouteIdList, Airline airline) {
        Either<ServiceError,PathResponse<PathAirline>> either = pathService.getPathAirlineList(originList,destinationList,excludeAirportList,excludeRouteIdList,airline);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private PathResponse<PathAirline> fetchPath(List<String> originList, List<String> destinationList) {
        Either<ServiceError,PathResponse<PathAirline>> either = pathService.getPathAirlineList(originList,destinationList);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private PathResponse<PathAirline> fetchPath(List<String> originList, List<String> destinationList, Airline airline) {
        Either<ServiceError, PathResponse<PathAirline>> either = pathService.getPathAirlineList(originList,destinationList,airline);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private Route fetchRoute(Integer id) {
        Either<ServiceError,Route> either = routeService.getRoute(id);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private ResultSearchFull fetchResultSearchFull(String sourceId) {
        Either<ServiceError,ResultSearchFull> either = searchService.fetchResultSearchFull(sourceId);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Airport> fetchNearbyAirports(double latitude, double longitude, int limit) {
        Either<ServiceError,List<Airport>> either = airportService.getNearbyAirports(longitude,latitude,limit);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Airport> fetchNearbyAirports(double latitude, double longitude, int limit, AirportType type) {
        Either<ServiceError,List<Airport>> either = airportService.getNearbyAirports(longitude,latitude,limit,type);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Airport> fetchNearbyAirports(double latitude, double longitude, int limit, Airline airline) {
        Either<ServiceError,List<Airport>> either = airportService.getNearbyAirports(longitude,latitude,limit,airline);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Airport> fetchNearbyAirports(double latitude, double longitude, int limit, List<Airline> airlineList) {
        Either<ServiceError,List<Airport>> either = airportService.getNearbyAirports(longitude,latitude,limit,airlineList);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }


    private List<Airport> fetchNearbyAirports(double latitude, double longitude, int limit, AirportType type, Airline airline) {
        Either<ServiceError, List<Airport>> either = airportService.getNearbyAirports(longitude, latitude, limit,type,airline);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Airport> fetchAirports() {
        Either<ServiceError,List<Airport>> either = airportService.getAirports();
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Airport> fetchAirports(AirportType type) {
        Either<ServiceError,List<Airport>> either = airportService.getAirports(type);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Airport> fetchAirports(Airline airline) {
        Either<ServiceError,List<Airport>> either = airportService.getAirports(airline);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Airport> fetchAirports(AirportType type,Airline airline) {
        Either<ServiceError,List<Airport>> either = airportService.getAirports(type,airline);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private void resolveServiceError(ServiceError serviceError) {
        LOGGER.error(serviceError.getException().getMessage());
        throw new ResponseStatusException(serviceError.getHttpStatus().getCode(),
                serviceError.getException().getMessage(), serviceError.getException());
    }
}
