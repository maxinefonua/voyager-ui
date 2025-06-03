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

import static org.voyager.utils.ConstantsUI.AIRPORT_FILTER_PARAM_NAME;

@Controller
public class TripsController {
//    TODO: make a trips service to separate logic from controller
    private static final TripFilter DEFAULT_TRIP_FILTER = TripFilter.LOCATION;
    private static final Status DEFAULT_LOCATION_STATUS_FILTER = Status.SAVED;
    private static final AirportFilter DEFAULT_AIRPORT_FILTER = AirportFilter.DELTA;
    private static final Logger LOGGER = LoggerFactory.getLogger(TripsController.class);

    @Autowired
    private VoyagerService voyagerService;

    void addDefaultAttributes(Model model) {
        model.addAttribute("selection",DEFAULT_TRIP_FILTER.name());
        model.addAttribute("filterList",Option.getFilterOptions(DEFAULT_TRIP_FILTER));
        if (DEFAULT_TRIP_FILTER.equals(TripFilter.LOCATION)) {
            populateLocationDefaults(model);
        } else if (DEFAULT_TRIP_FILTER.equals(TripFilter.AIRPORT)) {
            model.addAttribute("optionList", getDefaultAirportOptionListForInput());
        } else {
            LOGGER.error(String.format("addDefaultAttributes w %s - but not yet implemented",
                    DEFAULT_TRIP_FILTER.name()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal error fetching from selection options");
        }
    }

    @GetMapping("/radio-selection")
    public String selectFrom(Model model, @RequestParam(required = false) TripFilter tripFilterStart, @RequestParam(required = false) TripFilter tripFilterEnd, @RequestParam Boolean isStart) {
        TripFilter tripFilter = tripFilterStart != null ? tripFilterStart : tripFilterEnd;
        model.addAttribute("filterList",Option.getFilterOptions(tripFilter));
        model.addAttribute("selection",tripFilter.name());
        model.addAttribute("isStart",isStart);
        switch (tripFilter) {
            case LOCATION -> {
                populateLocationDefaults(model);
                return "fragments/routes :: by-location";
            }
            case AIRPORT -> {
                model.addAttribute("isStart",tripFilterStart != null);
                model.addAttribute("optionList", getDefaultAirportOptionListForInput());
                return "fragments/routes :: by-airport";
            }
            default -> {
                LOGGER.error(String.format("/radio-selection called w %s - but not yet implemented",
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

    @GetMapping("/trips/nearby-airports-location")
    public String nearbyAirports(Model model, @RequestParam Integer locationId,@RequestParam Boolean isStart,
                                 @RequestParam(AIRPORT_FILTER_PARAM_NAME) AirportFilter airportFilter) {
        LOGGER.info("nearbyAirports called with locationId: "+ locationId);
        List<Airport> nearbyAirports = new ArrayList<>();
        if (locationId != 0) {
            Location location = voyagerService.getLocation(locationId);
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            switch (airportFilter) {
                case PINNED -> {
                    for (String iata : location.getAirports())
                        nearbyAirports.add(voyagerService.getAirport(iata));
                }
                case DELTA ->
                        nearbyAirports.addAll(voyagerService.nearbyAirports(latitude, longitude, 5, Airline.DELTA));
                case CIVIL ->
                        nearbyAirports.addAll(voyagerService.nearbyAirports(latitude, longitude, 5, AirportType.CIVIL));
                case MILITARY ->
                        nearbyAirports.addAll(voyagerService.nearbyAirports(latitude, longitude, 5, AirportType.MILITARY));
                case ALL ->
                        nearbyAirports.addAll(voyagerService.nearbyAirports(latitude, longitude, 5, AirportType.CIVIL));
            }
        }
        List<Option> optionList = nearbyAirports.stream().map(airport -> buildAirportOption(airport,true)).toList();
        model.addAttribute("optionList",optionList);
        model.addAttribute("isStart",isStart);
        return "fragments/routes :: airport-select-options-for-location";
    }

    @GetMapping("/trip-options")
    public String getTripOptions(Model model, TripFilter selection, String optionFilter, Boolean isStart){
        LOGGER.debug(String.format("/trip-options called with tripfilter selection: %s, optionFilter %s",
                selection.name(),optionFilter));
        List<Option> optionList = new ArrayList<>();
        switch (selection) {
            case LOCATION -> {
                Status status = ValidationUtils.getLocationStatusElseDefault(optionFilter);
                List<Location> locationList = voyagerService.getLocations(status);
                List<Option> airportOptionList = new ArrayList<>();
                if (!locationList.isEmpty()) {
                    Location first = locationList.get(0);
                    first.getAirports().forEach(iata -> {
                        airportOptionList.add(buildAirportOption(voyagerService.getAirport(iata),true));
                    });
                }
                optionList.addAll(getLocationOptionsList(status));
            }
            case AIRPORT -> {
                AirportFilter airportFilter = ValidationUtils.getAirportFilterElseDefault(optionFilter);
                optionList.addAll(getAirportOptionsListForInput(airportFilter));
            }
        }
        model.addAttribute("isStart",isStart);
        model.addAttribute("optionList",optionList);
        return "fragments/options :: trip-select-options";
    }

    private List<Option> getAirportOptionsListForInput(AirportFilter airportFilter) {
        List<Airport> airportList = new ArrayList<>();
        switch (airportFilter) {
            case DELTA -> airportList.addAll(voyagerService.airports(Airline.DELTA));
            case CIVIL -> airportList.addAll(voyagerService.airports(AirportType.CIVIL));
            case MILITARY -> airportList.addAll(voyagerService.airports(AirportType.MILITARY));
            case ALL -> airportList.addAll(voyagerService.airports(Arrays.asList(AirportType.CIVIL,AirportType.MILITARY)));
        }
        return airportList.stream().map(airport -> buildAirportOption(airport,false)).toList();
    }

    private Option buildAirportOption(Airport airport,boolean forSelect) {
        if (forSelect) return buildAirportOptionForSelect(airport);
        return Option.builder()
                .elementName(airport.getIata())
                .display(String.format("%s | %s, %s of %s", airport.getName(), airport.getCity(),
                        airport.getSubdivision(),airport.getCountryCode()))
                .value(airport.getIata()).build();
    }

    private Option buildAirportOptionForSelect(Airport airport) {
        return Option.builder()
                .elementName(airport.getIata())
                .display(String.format("%s | %s", airport.getIata(), airport.getName()))
                .value(airport.getIata()).build();
    }

    private List<Option> getLocationOptionsList(Status status) {
        return voyagerService.getLocations(status).stream().map(location -> Option.builder()
                        .elementName(String.format("%s-%s", location.getName(),location.getId()))
                        .display(String.format("%s, %s in %s", location.getName(),
                                location.getSubdivision(),location.getCountryCode()))
                        .value(String.valueOf(location.getId().intValue())).build())
                .toList();
    }

    private List<Option> getDefaultAirportOptionListForInput() {
        List<Airport> airportList = new ArrayList<>();
        switch (DEFAULT_AIRPORT_FILTER) {
            case DELTA -> {
                airportList.addAll(voyagerService.airports(Airline.DELTA));
            }
            case ALL -> {
                airportList.addAll(voyagerService.airports(List.of(AirportType.CIVIL,
                        AirportType.MILITARY)));
            }
            case MILITARY -> {
                airportList.addAll(voyagerService.airports(AirportType.MILITARY));
            }
            case CIVIL -> {
                airportList.addAll(voyagerService.airports(AirportType.CIVIL));
            }
        }
        return airportList.stream()
                .map(airport -> buildAirportOption(airport,false))
                .toList();
    }



    private void populateLocationDefaults(Model model) {
        List<Location> locations = voyagerService.getLocations(DEFAULT_LOCATION_STATUS_FILTER);
        List<Option> optionList = locations.stream()
                .map(location -> Option.builder().elementName(String.format("%s-%s",
                                location.getName(),location.getId())).display(String.format("%s, %s in %s",
                                location.getName(),location.getSubdivision(), location.getCountryCode()))
                        .value(String.valueOf(location.getId().intValue())).build())
                .toList();
        List<Option> airportOptionList = new ArrayList<>();
        if (!locations.isEmpty()) airportOptionList.addAll(addAirportsFromLocation(locations.get(0)));
        model.addAttribute("optionList",optionList);
        model.addAttribute("airportOptionList",airportOptionList);
    }

    private List<Option> addAirportsFromLocation(Location location) {
        List<Option> airportOptionList = new ArrayList<>();
        for (String iata : location.getAirports())
            airportOptionList.add(buildAirportOption(voyagerService.getAirport(iata),true));
        return airportOptionList;
    }
}
