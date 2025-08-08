package org.voyager.service.impl;

import org.voyager.model.Airline;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.service.AirportService;

import java.util.List;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class AirportServiceAPI {
    private final AirportService airportService;
    AirportServiceAPI(AirportService airportService) {
        this.airportService = airportService;
    }

    public List<Airline> getAirlines(List<String> iataList) {
        return unwrapEither(this.airportService.getAirlines(iataList));
    }

    public Airport getAirport(String iata) {
        return unwrapEither(this.airportService.getAirport(iata));
    }

    public List<Airport> nearbyAirports(Double latitude, Double longitude, int limit, List<Airline> airlineList) {
        return unwrapEither(this.airportService.getNearbyAirports(longitude,latitude,limit,airlineList));
    }

    public List<Airport> nearbyAirports(Double latitude, Double longitude, int limit) {
        return unwrapEither(this.airportService.getNearbyAirports(longitude,latitude,limit));
    }

    public List<Airport> nearbyAirports(Double latitude, Double longitude, int limit, AirportType airportType) {
        return unwrapEither(this.airportService.getNearbyAirports(longitude,latitude,limit,airportType));
    }
}
