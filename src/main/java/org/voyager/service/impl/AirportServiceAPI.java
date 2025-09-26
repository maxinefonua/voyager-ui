package org.voyager.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.voyager.model.Airline;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.service.AirportService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;
import static org.voyager.utils.ConstantsUtils.ALPHA3_CODE_REGEX;

public class AirportServiceAPI {
    private final AirportService airportService;
    private List<Airport> allAirports;
    private Set<String> iataCodes;

    AirportServiceAPI(AirportService airportService) {
        this.airportService = airportService;
    }

    public List<Airline> getAirlines(List<String> iataList) {
        return unwrapEither(this.airportService.getAirlines(iataList));
    }

    public Airport getAirport(String iata) {
        if (allAirports == null) allAirports = unwrapEither(this.airportService.getAirports());
        Optional<Airport> exists = allAirports.stream().filter(airport ->
                airport.getIata().equals(iata)).findAny();
        if (exists.isPresent()) return exists.get();
        Airport airport = unwrapEither(this.airportService.getAirport(iata));
        allAirports.add(airport);
        iataCodes.add(airport.getIata());
        return airport.toBuilder().build();
    }

    public List<Airport> nearbyAirports(Double latitude, Double longitude, int limit, List<Airline> airlineList) {
        return unwrapEither(this.airportService.getNearbyAirports(longitude,latitude,limit,airlineList)).stream()
                .map(airport -> airport.toBuilder().build()).toList();
    }

    public boolean isValidIataCode(String airportCode) {
        if (allAirports == null) allAirports = unwrapEither(this.airportService.getAirports());
        if (iataCodes == null) iataCodes = allAirports.stream().map(Airport::getIata).collect(Collectors.toSet());
        if (StringUtils.isBlank(airportCode) || !airportCode.matches(ALPHA3_CODE_REGEX)) return false;
        return iataCodes.contains(airportCode);
    }
}
