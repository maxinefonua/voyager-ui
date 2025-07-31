package org.voyager.service.impl;

import org.voyager.model.airport.Airport;
import org.voyager.service.AirportService;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class AirportServiceAPI {
    private final AirportService airportService;
    AirportServiceAPI(AirportService airportService) {
        this.airportService = airportService;
    }

    public Airport getAirport(String iata) {
        return unwrapEither(this.airportService.getAirport(iata));
    }
}
