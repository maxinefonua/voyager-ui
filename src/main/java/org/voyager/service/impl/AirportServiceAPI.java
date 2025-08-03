package org.voyager.service.impl;

import org.voyager.model.Airline;
import org.voyager.model.airport.Airport;
import org.voyager.service.AirportService;

import java.util.List;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class AirportServiceAPI {
    private final AirportService airportService;
    AirportServiceAPI(AirportService airportService) {
        this.airportService = airportService;
    }

    public Airport getAirport(String iata) {
        return unwrapEither(this.airportService.getAirport(iata));
    }

    public List<Airport> nearbyAirports(Double latitude, Double longitude, int limit, Airline airline) {
        return unwrapEither(this.airportService.getNearbyAirports(longitude,latitude,limit,airline));
    }

    public List<Airport> nearbyAirports(Double latitude, Double longitude, int limit) {
        return unwrapEither(this.airportService.getNearbyAirports(longitude,latitude,limit));
    }
}
