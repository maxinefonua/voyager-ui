package org.voyager.service;

import org.voyager.model.Airline;
import org.voyager.model.ResultDetails;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.flight.Flight;
import org.voyager.model.location.*;
import org.voyager.model.response.SearchResult;
import org.voyager.model.result.LookupAttribution;
import org.voyager.model.result.ResultSearchFull;
import org.voyager.model.route.PathAirline;
import org.voyager.model.route.PathResponse;
import org.voyager.model.route.Route;
import org.voyager.service.impl.*;

import java.util.List;

public interface VoyagerService {
    FlightServiceAPI getFlightServiceAPI();
    CountryServiceAPI getCountryServiceAPI();
    LocationServiceAPI getLocationServiceAPI();
    SearchServiceAPI getSearchServiceAPI();
    AirportServiceAPI getAirportServiceAPI();
    PathServiceAPI getPathServiceAPI();
}
