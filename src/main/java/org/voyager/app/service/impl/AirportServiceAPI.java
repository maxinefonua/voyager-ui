package org.voyager.app.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.voyager.commons.constants.Regex;
import org.voyager.sdk.model.NearbyAirportQuery;
import org.voyager.commons.model.airport.Airport;
import org.voyager.sdk.service.AirportService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.voyager.app.service.impl.VoyagerServiceImpl.unwrapEither;

public class AirportServiceAPI {
    private final AirportService airportService;
    private final List<Airport> allAirports;
    private final Set<String> iataCodes;

    AirportServiceAPI(AirportService airportService) {
        this.airportService = airportService;
        allAirports = unwrapEither(this.airportService.getAirports());
        iataCodes = allAirports.stream().map(Airport::getIata).collect(Collectors.toSet());
    }

    public Airport getAirport(String iata) {
        Optional<Airport> exists = allAirports.stream().filter(airport ->
                airport.getIata().equals(iata)).findAny();
        if (exists.isPresent()) return exists.get();
        Airport airport = unwrapEither(this.airportService.getAirport(iata));
        allAirports.add(airport);
        iataCodes.add(airport.getIata());
        return airport.toBuilder().build();
    }

    public List<Airport> nearbyAirports(NearbyAirportQuery nearbyAirportQuery) {
        return unwrapEither(this.airportService.getNearbyAirports(nearbyAirportQuery)).stream()
                .map(airport -> airport.toBuilder().build()).toList();
    }

    public boolean isValidIataCode(String airportCode) {
        if (StringUtils.isBlank(airportCode) || !airportCode.matches(Regex.AIRPORT_CODE)) return false;
        return iataCodes.contains(airportCode);
    }
}
