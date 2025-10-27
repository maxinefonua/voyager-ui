package org.voyager.service.impl;

import org.voyager.sdk.model.FlightQuery;
import org.voyager.commons.model.flight.Flight;
import org.voyager.sdk.service.FlightService;

import java.util.List;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class FlightServiceAPI {
    private final FlightService flightService;

    FlightServiceAPI(FlightService flightService) {
        this.flightService = flightService;
    }

    public List<Flight> getFlights(FlightQuery flightQuery) {
        return unwrapEither(flightService.getFlights(flightQuery));
    }
}
