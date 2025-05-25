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
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.location.Location;
import org.voyager.model.location.LocationForm;
import org.voyager.model.response.SearchResult;
import org.voyager.model.result.LookupAttribution;
import org.voyager.model.result.ResultSearch;
import org.voyager.model.route.Path;
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
    private SearchService searchService;
    private LocationService locationService;

    private List<Airport> allAirports;
    private List<Airport> deltaAirports;
    private Map<String,Airport> airportMap;

    @PostConstruct
    public void init() {
        VoyagerConfig voyagerConfig = new VoyagerConfig(
                resolveProtocol(voyagerAPIConfig.getProtocol()),
                voyagerAPIConfig.getHost(),
                voyagerAPIConfig.getPort(),
                voyagerAPIConfig.getMaxThreads(),
                voyagerAPIConfig.getAuthToken());

        Voyager voyager = new Voyager(voyagerConfig);
        this.airportService = voyager.getAirportService();
        this.routeService = voyager.getRouteService();
        this.searchService = voyager.getSearchService();
        this.locationService = voyager.getLocationService();

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
    public List<Location> getLocations() {
        return fetchLocations();
    }

    @Override
    public Location getLocationById(Integer id) {
        return fetchLocation(id);
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
    public List<Airport> airports(@NonNull Airline airline) {
        if (airline.equals(Airline.DELTA)) return deltaAirports;
        return fetchAirports(airline);
    }

    @Override
    public List<Airport> airports() {
        return allAirports;
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
    public Path getPath(String origin, String destination) {
        return fetchPath(origin,destination);
    }

    @Override
    public Path getPath(String origin, String destination, Set<String> exclusions) {
        return fetchPath(origin,destination,exclusions);
    }

    private Location createLocation(LocationForm locationForm) {
        Either<ServiceError, Location> either = locationService.createLocation(locationForm);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private Location fetchLocation(Integer id) {
        Either<ServiceError, Location> either = locationService.getLocation(id);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private List<Location> fetchLocations() {
        Either<ServiceError, List<Location>> either = locationService.getLocations();
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

    private Path fetchPath(String origin, String destination, Set<String> exclusions) {
        Either<ServiceError,Path> either = routeService.getPath(origin,destination,exclusions);
        if (either.isLeft()) resolveServiceError(either.getLeft());
        return either.get();
    }

    private Path fetchPath(String origin, String destination) {
        Either<ServiceError,Path> either = routeService.getPath(origin,destination);
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
