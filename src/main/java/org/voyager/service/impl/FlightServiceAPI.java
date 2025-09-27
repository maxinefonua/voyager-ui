package org.voyager.service.impl;

import org.voyager.model.Airline;
import org.voyager.model.flight.Flight;
import org.voyager.service.FlightService;

import java.util.List;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class FlightServiceAPI {
    private final FlightService flightService;

    FlightServiceAPI(FlightService flightService) {
        this.flightService = flightService;
    }

    public List<Flight> getFlights(Integer routeId, Airline airline) {
        return unwrapEither(flightService.getFlights(routeId,true,airline));
    }
}
