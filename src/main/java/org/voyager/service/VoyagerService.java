package org.voyager.service;

import org.voyager.service.impl.*;

public interface VoyagerService {
    FlightServiceAPI getFlightServiceAPI();
    CountryServiceAPI getCountryServiceAPI();
    LocationServiceAPI getLocationServiceAPI();
    SearchServiceAPI getSearchServiceAPI();
    AirportServiceAPI getAirportServiceAPI();
    PathServiceAPI getPathServiceAPI();
    AirlineServiceAPI getAirlineServiceAPI();
}
