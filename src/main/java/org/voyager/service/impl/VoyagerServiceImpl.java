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

import static org.voyager.utils.ConstantsUtils.ALPHA3_CODE_REGEX;

@Service
public class VoyagerServiceImpl implements VoyagerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoyagerServiceImpl.class);

    @Autowired
    VoyagerAPIConfig voyagerAPIConfig;

    private Voyager voyager;

    private FlightServiceAPI flightServiceAPI;
    private CountryServiceAPI countryServiceAPI;
    private LocationServiceAPI locationServiceAPI;
    private SearchServiceAPI searchServiceAPI;
    private AirportServiceAPI airportServiceAPI;
    private PathServiceAPI pathServiceAPI;

    @PostConstruct
    public void init() {
        this.voyager = new Voyager(voyagerAPIConfig.getVoyagerConfig());
    }

    @Override
    public FlightServiceAPI getFlightServiceAPI() {
        if (flightServiceAPI == null) flightServiceAPI = new FlightServiceAPI(voyager.getFlightService());
        return flightServiceAPI;
    }

    @Override
    public CountryServiceAPI getCountryServiceAPI() {
        if (countryServiceAPI == null) countryServiceAPI = new CountryServiceAPI(voyager.getCountryService());
        return countryServiceAPI;
    }

    @Override
    public LocationServiceAPI getLocationServiceAPI() {
        if (locationServiceAPI == null) locationServiceAPI = new LocationServiceAPI(voyager.getLocationService());
        return locationServiceAPI;
    }

    @Override
    public SearchServiceAPI getSearchServiceAPI() {
        if (searchServiceAPI == null) searchServiceAPI = new SearchServiceAPI(voyager.getSearchService());
        return searchServiceAPI;
    }

    @Override
    public AirportServiceAPI getAirportServiceAPI() {
        if (airportServiceAPI == null) airportServiceAPI = new AirportServiceAPI(voyager.getAirportService());
        return airportServiceAPI;
    }

    @Override
    public PathServiceAPI getPathServiceAPI() {
        if (pathServiceAPI == null) pathServiceAPI = new PathServiceAPI(voyager.getPathService());
        return pathServiceAPI;
    }

    static <T> T unwrapEither(Either<ServiceError, T> either) {
        if (either.isLeft()) {
            ServiceError serviceError = either.getLeft();
            Exception exception = serviceError.getException();
            LOGGER.error(exception.getMessage());
            throw new ResponseStatusException(serviceError.getHttpStatus().getCode(),
                    exception.getMessage(), exception);
        }
        return either.get();
    }
}
