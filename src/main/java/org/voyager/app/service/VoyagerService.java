package org.voyager.app.service;

import org.voyager.app.service.impl.*;

public interface VoyagerService {
    FlightServiceAPI getFlightServiceAPI();
    CountryServiceAPI getCountryServiceAPI();
    LocationServiceAPI getLocationServiceAPI();
    SearchServiceAPI getSearchServiceAPI();
    AirportServiceAPI getAirportServiceAPI();
    PathServiceAPI getPathServiceAPI();
    AirlineServiceAPI getAirlineServiceAPI();
}
