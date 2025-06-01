package org.voyager.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.model.Airline;
import org.voyager.model.AirportFilter;
import org.voyager.model.Option;
import org.voyager.model.TripFilter;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.location.Location;
import org.voyager.model.location.Status;
import org.voyager.service.VoyagerService;
import org.voyager.validate.ValidationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class TripsController {
//    TODO: make a trips service to separate logic from controller
    private static final TripFilter DEFAULT_TRIP_FILTER = TripFilter.LOCATION;
    private static final Logger LOGGER = LoggerFactory.getLogger(TripsController.class);

    @Autowired
    private VoyagerService voyagerService;

    void addDefaultAttributes(Model model) {
        model.addAttribute("selection",DEFAULT_TRIP_FILTER.name());
        model.addAttribute("filterList",Option.getFilterOptions(DEFAULT_TRIP_FILTER));
        model.addAttribute("optionList",getOptionsList(DEFAULT_TRIP_FILTER));
    }

    @GetMapping("/from-selection")
    public String selectFrom(Model model, @RequestParam TripFilter tripFilter) {
        model.addAttribute("filterList",Option.getFilterOptions(tripFilter));
        model.addAttribute("optionList",getOptionsList(tripFilter));
        model.addAttribute("selection",tripFilter.name());
        switch (tripFilter) {
            case LOCATION -> { return "fragments/routes :: start-by-location"; }
            case AIRPORT -> { return "fragments/routes :: start-by-airport"; }
            default -> {
                LOGGER.error(String.format("/from-selection called w %s - but not yet implemented",
                        tripFilter.name()));
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal error fetching from selection options");
            }
        }
    }

    @GetMapping("/trips")
    public String getTrips(Model model) {
        List<Location> locations = voyagerService.getLocations();
        model.addAttribute("locations",locations);
        model.addAttribute("lookupAttribution", voyagerService.lookupAttribution());
        addDefaultAttributes(model);
        return "fragments/tab :: trips-tab";
    }

    @GetMapping("/trip-options")
    public String getTripOptions(Model model, TripFilter selection, String optionFilter){
        LOGGER.debug(String.format("/trip-options called with tripfilter selection: %s, optionFilter %s",
                selection.name(),optionFilter));
        List<Option> optionList = new ArrayList<>();
                switch (selection) {
            case LOCATION -> {
                Status status = ValidationUtils.getLocationStatusElseDefault(optionFilter);
                optionList.addAll(getLocationOptionsList(status));
            }
            case AIRPORT -> {
                AirportFilter airportFilter = ValidationUtils.getAirportFilterElseDefault(optionFilter);
                optionList.addAll(getAirportOptionsList(airportFilter));
            }
        }
        model.addAttribute("optionList",optionList);
        return "fragments/options :: trip-select-options";
    }

    private List<Option> getAirportOptionsList(AirportFilter airportFilter) {
        List<Airport> airportList = new ArrayList<>();
        switch (airportFilter) {
            case DELTA -> airportList.addAll(voyagerService.airports(Airline.DELTA));
            case CIVIL -> airportList.addAll(voyagerService.airports(AirportType.CIVIL));
            case MILITARY -> airportList.addAll(voyagerService.airports(AirportType.MILITARY));
            case ALL -> airportList.addAll(voyagerService.airports(Arrays.asList(AirportType.CIVIL,AirportType.MILITARY)));
        }
        return airportList.stream().map(airport -> Option.builder()
                        .elementName(airport.getIata())
                        .display(String.format("%s | %s, %s of %s", airport.getName(), airport.getCity(),
                                airport.getSubdivision(),airport.getCountryCode()))
                        .value(airport.getIata()).build())
                .toList();
    }

    private List<Option> getLocationOptionsList(Status status) {
        return voyagerService.getLocations(status).stream().map(location -> Option.builder()
                        .elementName(String.format("%s-%s", location.getName(),location.getId()))
                        .display(String.format("%s, %s in %s", location.getName(),
                                location.getSubdivision(),location.getCountryCode()))
                        .value(String.valueOf(location.getId().intValue())).build())
                .toList();
    }

    private List<Option> getOptionsList(TripFilter filter) {
        List<Option> datalistOptions = new ArrayList<>();
        switch (filter) {
            case AIRPORT -> {
                datalistOptions = voyagerService.airports(Airline.DELTA).stream()
                        .map(airport -> Option.builder().elementName(airport.getIata()).display(
                                        String.format("%s | %s, %s of %s", airport.getName(), airport.getCity(),
                                                airport.getSubdivision(),airport.getCountryCode()))
                                .value(airport.getIata()).build())
                        .toList();
            }
            case LOCATION -> {
                datalistOptions = voyagerService.getLocations(Status.SAVED).stream()
                        .map(location -> Option.builder().elementName(String.format("%s-%s",
                                        location.getName(),location.getId())).display(String.format("%s, %s in %s",
                                        location.getName(),location.getSubdivision(), location.getCountryCode()))
                                .value(String.valueOf(location.getId().intValue())).build())
                        .toList();
            }
        }
        return datalistOptions;
    }
}
