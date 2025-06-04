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
import org.springframework.web.servlet.ModelAndView;
import org.voyager.model.*;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.location.Location;
import org.voyager.model.location.Status;
import org.voyager.service.VoyagerService;
import org.voyager.validate.ValidationUtils;

import java.util.*;

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
        model.addAttribute("tripFilterStart",DEFAULT_TRIP_FILTER.name());
        model.addAttribute("tripFilterEnd",DEFAULT_TRIP_FILTER.name());
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

    @GetMapping("/build-path")
    public String buildPath(Model model, Integer startLocationId, Integer endLocationId,
                            String startAirport, String endAirport,
                            TripFilter tripFilterStart, TripFilter tripFilterEnd) {
        if (startLocationId != null && startLocationId != 0)
            model.addAttribute("startLocation",voyagerService.getLocation(startLocationId));
        if (endLocationId != null && endLocationId != 0)
            model.addAttribute("endLocation",voyagerService.getLocation(endLocationId));
        // toUpperCase added to allow firing from airport input (doesn't change to uppercase until after valid match found)
        if (voyagerService.isValidIataCode(startAirport.toUpperCase()))
            model.addAttribute("startAirport",voyagerService.getAirport(startAirport.toUpperCase()));
        if (voyagerService.isValidIataCode(endAirport.toUpperCase()))
            model.addAttribute("endAirport",voyagerService.getAirport(endAirport.toUpperCase()));
        model.addAttribute("tripFilterStart",tripFilterStart.name());
        model.addAttribute("tripFilterEnd",tripFilterEnd.name());
        return "fragments/routes :: review-path";
    }

    @GetMapping("/radio-selection")
    public Collection<ModelAndView> selectFrom(Model model,
                                               TripFilter tripFilterStart, TripFilter tripFilterEnd,
                                               Integer startLocationId, Integer endLocationId,
                                               String startAirport, String endAirport,
                                               @RequestParam Boolean isStart) {
        TripFilter tripFilter = isStart ? tripFilterStart : tripFilterEnd;
        model.addAttribute("filterList",Option.getFilterOptions(tripFilter));
        model.addAttribute("selection",tripFilter.name());
        model.addAttribute("isStart",isStart);


        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport);
        if (isStart) {
            reviewPathAttributes.put("tripFilterStart",tripFilter.name());
            reviewPathAttributes.remove("startLocation");
            reviewPathAttributes.remove("startAirport");
        } else {
            reviewPathAttributes.put("tripFilterEnd",tripFilter.name());
            reviewPathAttributes.remove("endLocation");
            reviewPathAttributes.remove("endAirport");
        }

        switch (tripFilter) {
            case LOCATION -> {
                populateLocationDefaults(model);
                return List.of(new ModelAndView("fragments/routes :: by-location",model.asMap()),
                        new ModelAndView("fragments/routes :: review-path",reviewPathAttributes));
            }
            case AIRPORT -> {
                model.addAttribute("isStart",isStart);
                model.addAttribute("optionList", getDefaultAirportOptionListForInput());
                return List.of(new ModelAndView("fragments/routes :: by-airport",model.asMap()),
                        new ModelAndView("fragments/routes :: review-path",reviewPathAttributes));
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
    public Collection<ModelAndView> nearbyAirports(Model model,
                                 Integer startLocationId, Integer endLocationId,
                                 String startAirport, String endAirport,
                                 @RequestParam Boolean isStart,
                                 @RequestParam(AIRPORT_FILTER_PARAM_NAME) AirportFilter airportFilter) {
        LOGGER.info("nearbyAirports called with startLocationId: "+ startLocationId + ", endLocationId: " + endLocationId);

        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport);

        Location location = null;
        if (isStart) location = voyagerService.getLocation(startLocationId);
        else location = voyagerService.getLocation(endLocationId);

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        List<Airport> nearbyAirports = new ArrayList<>();
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
        List<Option> optionList = nearbyAirports.stream().map(airport -> buildAirportOption(airport,true)).toList();
        model.addAttribute("optionList",optionList);
        model.addAttribute("isStart",isStart);
        return List.of(new ModelAndView("fragments/routes :: airport-select-options-for-location",model.asMap()),
                new ModelAndView("fragments/routes :: review-path",reviewPathAttributes));
    }

    @GetMapping("/trip-options")
    public Collection<ModelAndView> getTripOptions(Model model, TripFilter selection,
                                                   Integer startLocationId, Integer endLocationId,
                                                   String startAirport, String endAirport,
                                                   String optionFilter, Boolean isStart){
        LOGGER.debug(String.format("/trip-options called with tripfilter selection: %s, optionFilter %s",
                selection.name(),optionFilter));
        List<Option> optionList = new ArrayList<>();
        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport);

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
                    if (isStart) {
                        reviewPathAttributes.put("startLocation", first);
                        reviewPathAttributes.remove("startAirport");
                    }
                    else {
                        reviewPathAttributes.put("endLocation",first);
                        reviewPathAttributes.remove("endAirport");
                    }
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
        return List.of(new ModelAndView("fragments/options :: trip-select-options",model.asMap()),
                new ModelAndView("fragments/routes :: review-path",reviewPathAttributes));
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

    private Map<String,Object> populateReviewPathAttributes(Integer startLocationId, Integer endLocationId,
                                                            String startAirport, String endAirport) {
        Map<String,Object> reviewPathAttributes = new HashMap<>();
        if (startLocationId == null) {
            reviewPathAttributes.put("tripFilterStart", TripFilter.AIRPORT.name());
        } else {
            reviewPathAttributes.put("tripFilterStart",TripFilter.LOCATION.name());
            if (startLocationId != 0)
                reviewPathAttributes.put("startLocation",voyagerService.getLocation(startLocationId));
        }

        if (endLocationId == null) {
            reviewPathAttributes.put("tripFilterEnd",TripFilter.AIRPORT.name());
        } else {
            reviewPathAttributes.put("tripFilterEnd",TripFilter.LOCATION.name());
            if (endLocationId != 0)
                reviewPathAttributes.put("endLocation",voyagerService.getLocation(endLocationId));
        }

        if (voyagerService.isValidIataCode(startAirport))
            reviewPathAttributes.put("startAirport",voyagerService.getAirport(startAirport));
        if (voyagerService.isValidIataCode(endAirport))
            reviewPathAttributes.put("endAirport",voyagerService.getAirport(endAirport));
        return reviewPathAttributes;
    }


}
