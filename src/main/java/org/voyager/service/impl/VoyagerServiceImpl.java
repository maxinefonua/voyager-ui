package org.voyager.service.impl;

import io.vavr.control.Either;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.config.VoyagerAPIConfig;
import org.voyager.commons.error.ServiceError;
import org.voyager.sdk.service.AirlineService;
import org.voyager.sdk.service.CountryService;
import org.voyager.sdk.service.FlightService;
import org.voyager.sdk.service.AirportService;
import org.voyager.sdk.service.LocationService;
import org.voyager.sdk.service.SearchService;
import org.voyager.sdk.service.PathService;
import org.voyager.sdk.service.impl.VoyagerServiceRegistry;
import org.voyager.service.*;

@Service
public class VoyagerServiceImpl implements VoyagerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoyagerServiceImpl.class);

    @Autowired
    VoyagerAPIConfig voyagerAPIConfig;

    private VoyagerServiceRegistry voyagerServiceRegistry;

    private FlightServiceAPI flightServiceAPI;
    private CountryServiceAPI countryServiceAPI;
    private LocationServiceAPI locationServiceAPI;
    private SearchServiceAPI searchServiceAPI;
    private AirportServiceAPI airportServiceAPI;
    private AirlineServiceAPI airlineServiceAPI;
    private PathServiceAPI pathServiceAPI;

    @PostConstruct
    public void init() {
        VoyagerServiceRegistry.initialize(voyagerAPIConfig.getVoyagerConfig());
        this.voyagerServiceRegistry = VoyagerServiceRegistry.getInstance();
    }

    @Override
    public FlightServiceAPI getFlightServiceAPI() {
        if (flightServiceAPI == null) {
            flightServiceAPI = new FlightServiceAPI(voyagerServiceRegistry.get(FlightService.class));
        }
        return flightServiceAPI;
    }

    @Override
    public CountryServiceAPI getCountryServiceAPI() {
        if (countryServiceAPI == null) {
            countryServiceAPI = new CountryServiceAPI(voyagerServiceRegistry.get(CountryService.class));
        }
        return countryServiceAPI;
    }

    @Override
    public LocationServiceAPI getLocationServiceAPI() {
        if (locationServiceAPI == null) {
            locationServiceAPI = new LocationServiceAPI(voyagerServiceRegistry.get(LocationService.class));
        }
        return locationServiceAPI;
    }

    @Override
    public SearchServiceAPI getSearchServiceAPI() {
        if (searchServiceAPI == null) {
            searchServiceAPI = new SearchServiceAPI(voyagerServiceRegistry.get(SearchService.class));
        }
        return searchServiceAPI;
    }

    @Override
    public AirportServiceAPI getAirportServiceAPI() {
        if (airportServiceAPI == null) {
            airportServiceAPI = new AirportServiceAPI(voyagerServiceRegistry.get(AirportService.class));
        }
        return airportServiceAPI;
    }

    @Override
    public PathServiceAPI getPathServiceAPI() {
        if (pathServiceAPI == null) {
            pathServiceAPI = new PathServiceAPI(voyagerServiceRegistry.get(PathService.class));
        }
        return pathServiceAPI;
    }

    @Override
    public AirlineServiceAPI getAirlineServiceAPI() {
        if (airlineServiceAPI == null) {
            airlineServiceAPI = new AirlineServiceAPI(voyagerServiceRegistry.get(AirlineService.class));
        }
        return airlineServiceAPI;
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
