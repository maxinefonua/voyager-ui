package org.voyager.app.service.impl;

import org.voyager.sdk.model.AirlineQuery;
import org.voyager.commons.model.airline.Airline;
import org.voyager.commons.model.airport.Airport;
import org.voyager.sdk.service.AirlineService;
import java.util.List;
import java.util.Set;

import static org.voyager.app.service.impl.VoyagerServiceImpl.unwrapEither;

public class AirlineServiceAPI {
    private final AirlineService airlineService;
    private List<Airport> allAirports;
    private Set<String> iataCodes;

    AirlineServiceAPI(AirlineService airlineService) {
        this.airlineService = airlineService;
    }

    public List<Airline> getAirlines(AirlineQuery airlineQuery) {
        return unwrapEither(this.airlineService.getAirlines(airlineQuery));
    }
}
