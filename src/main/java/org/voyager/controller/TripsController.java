package org.voyager.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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

import java.util.*;

import static org.voyager.utils.ConstantsUI.AIRPORT_FILTER_PARAM_NAME;

@Controller
public class TripsController {
//    TODO: make a trips service to separate logic from controller
    private static final TripFilter DEFAULT_TRIP_FILTER = TripFilter.LOCATION;
    private static final Status DEFAULT_LOCATION_STATUS_FILTER = Status.SAVED;
    private static final AirportFilter DEFAULT_AIRPORT_FILTER = AirportFilter.CIVIL;
    private static final AirportFilter DEFAULT_CLOSER_FILTER = AirportFilter.CIVIL;
    private static final Logger LOGGER = LoggerFactory.getLogger(TripsController.class);

    @Autowired
    private VoyagerService voyagerService;

    void addDefaultAttributes(Model model) {
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

    @GetMapping("/reverse-path")
    @Cacheable("reverseCache")
    public Collection<ModelAndView> reversePath(Model model, Integer startLocationId, Integer endLocationId,
                                                String startAirport, String endAirport,
                                                String closerStartAirport, String closerEndAirport,
                                                @RequestParam(name = "airportFilter") List<AirportFilter> airportFilters,
                                                AirportFilter closerFilterStart, AirportFilter closerFilterEnd,
                                                @RequestParam(name = "locationFilter",required = false) List<Status> locationFilters) {
        ModelAndView reversedPathMav = new ModelAndView(
                buildPath(model,endLocationId,startLocationId,endAirport,startAirport,closerEndAirport,closerStartAirport),
                model.asMap());

        Status locationFilterStart = null;
        Status locationFilterEnd = null;
        TripFilter tripFilterEnd = null;
        TripFilter tripFilterStart = null;
        if (startLocationId != null) {
            tripFilterStart = TripFilter.LOCATION;
            locationFilterStart = locationFilters.get(0);
        } else tripFilterStart = TripFilter.AIRPORT;
        if (endLocationId != null) {
            tripFilterEnd = TripFilter.LOCATION;
            locationFilterEnd = locationFilters.get(locationFilters.size()-1);
        } else tripFilterEnd = TripFilter.AIRPORT;

        ModelAndView startMav = reverseTrip(true,tripFilterEnd,locationFilterEnd,endLocationId,
                airportFilters.get(airportFilters.size()-1),endAirport,closerFilterEnd,closerEndAirport);
        ModelAndView endMav = reverseTrip(false,tripFilterStart,locationFilterStart,startLocationId,
                airportFilters.get(0),startAirport,closerFilterStart,closerStartAirport);
        return List.of(reversedPathMav,startMav,endMav);
    }

    @GetMapping("/build-path")
    public String buildPath(Model model, Integer startLocationId, Integer endLocationId,
                            String startAirport, String endAirport,
                            String closerStartAirport, String closerEndAirport) {
        model.addAllAttributes(populateReviewPathAttributes(startLocationId,endLocationId,startAirport,endAirport,closerStartAirport,closerEndAirport));
        return "fragments/routes :: review-path";
    }

    @GetMapping("/radio-selection")
    public Collection<ModelAndView> selectFrom(Model model,
                                               TripFilter tripFilterStart, TripFilter tripFilterEnd,
                                               Integer startLocationId, Integer endLocationId,
                                               String startAirport, String endAirport,
                                               String closerStartAirport, String closerEndAirport,
                                               @RequestParam Boolean isStart) {
        TripFilter tripFilter = isStart ? tripFilterStart : tripFilterEnd;
        model.addAttribute("selection",tripFilter.name());
        model.addAttribute("isStart",isStart);
        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport,closerStartAirport,closerEndAirport);
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

    @GetMapping("/trips/airport-input")
    public Collection<ModelAndView> airportInput(Model model,
                                                 Integer startLocationId, Integer endLocationId,
                                                 String startAirport, String endAirport,
                                                 String startAirportCode, String endAirportCode,
                                                 String closerStartAirport, String closerEndAirport,
                                                 @RequestParam Boolean isStart) {
        LOGGER.debug("airportInput called with startAirportCode: "+ startAirportCode + ", endAirportCode: " + endAirportCode);
        // returns updated input section

        String airportCode = null;
        String nonDeltaCode = null;
        AirportFilter closerFilter = null;

        if (isStart) {
            startAirportCode = startAirportCode.toUpperCase();
            if (voyagerService.isDeltaIataCode(startAirportCode)) {
                airportCode = startAirportCode;
                startAirport = startAirportCode;
            } else if (voyagerService.isValidIataCode(startAirportCode)) {
                nonDeltaCode = startAirportCode;
                closerStartAirport = startAirportCode;
                startAirport = null;
                closerFilter = AirportFilter.DELTA;
            }
        } else {
            endAirportCode = endAirportCode.toUpperCase();
            if (voyagerService.isDeltaIataCode(endAirportCode)) {
                airportCode = endAirportCode;
                endAirport = endAirportCode;
            } else if (voyagerService.isValidIataCode(endAirportCode)) {
                nonDeltaCode = endAirportCode;
                closerEndAirport = endAirportCode;
                endAirport = null;
                closerFilter = AirportFilter.DELTA;
            }
        }

        List<Option> airportOptionList = null;
        if (voyagerService.isValidIataCode(nonDeltaCode)) {
            Airport nonDeltaAirport = voyagerService.getAirport(nonDeltaCode);
            List<Airport> nearbyDelta = voyagerService.nearbyAirports(nonDeltaAirport.getLatitude(),
                    nonDeltaAirport.getLongitude(),10,Airline.DELTA);
            airportOptionList = nearbyDelta.stream().map(this::buildAirportOptionForSelect).toList();
        }

        model.addAttribute("airportCode",airportCode);
        model.addAttribute("nonDeltaCode",nonDeltaCode);
        model.addAttribute("isStart",isStart);
        model.addAttribute("airportOptionList",airportOptionList);
        model.addAttribute("closerFilter",closerFilter);

        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport,closerStartAirport,closerEndAirport);

        return List.of(
                new ModelAndView("fragments/routes :: update-airport-input",model.asMap()),
                new ModelAndView("fragments/routes :: review-path",reviewPathAttributes)
        );
    }

    @GetMapping("/trips/location-selected")
    public Collection<ModelAndView> locationSelected(Model model,
                                                   Integer startLocationId, Integer endLocationId,
                                                   String startAirport, String endAirport,
                                                   String closerStartAirport, String closerEndAirport,
                                                     AirportFilter closerFilterStart, AirportFilter closerFilterEnd,
                                                   @RequestParam Boolean isStart,
                                                   @RequestParam(AIRPORT_FILTER_PARAM_NAME) AirportFilter airportFilter) {
        LOGGER.debug("nearbyAirports called with startLocationId: "+ startLocationId + ", endLocationId: " + endLocationId);
        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport,closerStartAirport,closerEndAirport);
        if (isStart) {
            reviewPathAttributes.remove("startAirport");
            reviewPathAttributes.remove("closerStartAirport");
        } else {
            reviewPathAttributes.remove("endAirport");
            reviewPathAttributes.remove("closerEndAirport");
        }

        List<Option> closerAirportOptionList = null;
        AirportFilter closerFilter = null;
        if ((isStart && startLocationId != 0) || (!isStart && endLocationId != 0)) {
            Location location = null;
            if (isStart) {
                location = voyagerService.getLocation(startLocationId);
                closerFilter = closerFilterStart;
            } else {
                location = voyagerService.getLocation(endLocationId);
                closerFilter = closerFilterEnd;
            }
            List<Airport> nearbyAirports = getAirports(location,airportFilter);
            List<Option> optionList = nearbyAirports.stream().map(airport -> buildAirportOption(airport, true)).toList();
            model.addAttribute("optionList", optionList);
            if (airportFilter == AirportFilter.PINNED || airportFilter == AirportFilter.PINNED_DELTA
                || airportFilter == AirportFilter.PINNED_NONDELTA) {
                for (Airport pinned : nearbyAirports) {
                    pinned.setDistance(Airport.calculateDistance(location.getLatitude(),location.getLongitude(),
                            pinned.getLatitude(),pinned.getLongitude()));
                }
            }
            List<Airport> closerAirports = getCloserAirports(location,nearbyAirports.get(0),DEFAULT_CLOSER_FILTER);
            if (!closerAirports.isEmpty()) {
                closerAirportOptionList = closerAirports.stream().limit(10)
                        .map(this::buildAirportOptionForSelect).toList();
            }
        }
        Map<String,Object> closerAttributes = new HashMap<>();
        closerAttributes.put("closerAirportOptionList",closerAirportOptionList);
        if (closerFilter != null) closerAttributes.put("closerFilter",closerFilter.name());
        else closerAttributes.put("closerFilter",closerFilter);
        closerAttributes.put("isStart",isStart);

        model.addAttribute("isStart",isStart);
        return List.of(
                new ModelAndView("fragments/routes :: airport-select-options-for-location",model.asMap()),
                new ModelAndView("fragments/routes :: review-path",reviewPathAttributes),
                new ModelAndView("fragments/routes :: closer-airports",closerAttributes)
        );
    }

    private List<Airport> getCloserAirports(Location location,Airport closest,AirportFilter closerFilter) {
        List<Airport> airports = getAirports(location,closerFilter);
        List<Airport> closerAirports = new ArrayList<>();

        double minDistance = Airport.calculateDistance(location.getLatitude(),location.getLongitude(),
                closest.getLatitude(),closest.getLongitude());

        for (Airport airport : airports) {
            if (airport.getDistance() == null || closest.getDistance() == null) {
                double distance = Airport.calculateDistance(location.getLatitude(),location.getLongitude(),
                        airport.getLatitude(),airport.getLongitude());
                if (distance < minDistance)
                    closerAirports.add(airport);
            } else if (airport.getDistance() < closest.getDistance())
                closerAirports.add(airport);
        }
        return closerAirports;
    }

    @GetMapping("/trips/closer-airports-location")
    public Collection<ModelAndView> closerAirports(Model model,
                                                   Integer startLocationId, Integer endLocationId,
                                                   String startAirport, String endAirport,
                                                   String closerStartAirport, String closerEndAirport,
                                                   @RequestParam Boolean isStart,
                                                   @RequestParam AirportFilter airportFilter,
                                                   AirportFilter closerFilterStart, AirportFilter closerFilterEnd) {
        LOGGER.debug("closerAirports called with airportFilter: "+ airportFilter);
        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport,closerStartAirport,closerEndAirport);
        if ((isStart && startLocationId != 0) || (!isStart && endLocationId != 0)) {
            Location location = null;
            String airportCode = null;
            if (isStart) {
                location = voyagerService.getLocation(startLocationId);
                airportCode = startAirport;
            } else {
                location = voyagerService.getLocation(endLocationId);
                airportCode = endAirport;
            }
            Airport closestAirport = getAirports(location,airportFilter).get(0);
            AirportFilter closerFilter = isStart ? closerFilterStart : closerFilterEnd;
            List<Airport> airports = getCloserAirports(location,closestAirport,closerFilter);
            List<Option> closerAirportOptionList = airports.stream().map(this::buildAirportOptionForSelect).toList();
            model.addAttribute("closerAirportOptionList",closerAirportOptionList);
        }
        model.addAttribute("isStart",isStart);
        return List.of(new ModelAndView("fragments/routes :: closer-airport-options-for-location",model.asMap()),
                new ModelAndView("fragments/routes :: review-path",reviewPathAttributes));
    }

    @GetMapping("/trips/nearby-airports-location")
    public Collection<ModelAndView> nearbyAirports(Model model,
                                 Integer startLocationId, Integer endLocationId,
                                 String startAirport, String endAirport,
                                 String closerStartAirport, String closerEndAirport,
                                 @RequestParam Boolean isStart,
                                 @RequestParam(AIRPORT_FILTER_PARAM_NAME) AirportFilter airportFilter) {
        LOGGER.debug("nearbyAirports called with startLocationId: "+ startLocationId + ", endLocationId: " + endLocationId);

        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport,closerStartAirport,closerEndAirport);

        if ((isStart && startLocationId != 0) || (!isStart && endLocationId != 0)) {
            Location location = null;
            if (isStart) location = voyagerService.getLocation(startLocationId);
            else location = voyagerService.getLocation(endLocationId);

            List<Airport> nearbyAirports = getAirports(location,airportFilter);
            List<Option> optionList = nearbyAirports.stream().map(airport -> buildAirportOption(airport, true)).toList();
            model.addAttribute("optionList", optionList);
        }
        model.addAttribute("isStart",isStart);
        return List.of(new ModelAndView("fragments/routes :: airport-select-options-for-location",model.asMap()),
                new ModelAndView("fragments/routes :: review-path",reviewPathAttributes));
    }

    @GetMapping("/airport-options")
    public Collection<ModelAndView> getAirportOptions(Model model,
                                                      Integer startLocationId, Integer endLocationId,
                                                      String startAirport, String endAirport,
                                                      String closerStartAirport, String closerEndAirport,
                                                      AirportFilter airportFilter,
                                                      Boolean isStart){
        LOGGER.debug(String.format("/airport-options called with airportFilter %s",
                airportFilter.name()));
        List<Option> optionList = getAirportOptionsListForInput(airportFilter);
        model.addAttribute("isStart",isStart);
        model.addAttribute("optionList",optionList);

        Map<String,Object> airportInputAttributes = new HashMap<>();
        airportInputAttributes.put("isStart",isStart);

        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport,closerStartAirport,closerEndAirport);

        return List.of(new ModelAndView("fragments/options :: trip-select-options",model.asMap()),
                new ModelAndView("fragments/routes :: review-path",reviewPathAttributes),
                new ModelAndView("fragments/routes :: update-airport-input",airportInputAttributes));
    }

    @GetMapping("/location-options")
    public Collection<ModelAndView> getLocationOptions(Model model,
                                                       Integer startLocationId, Integer endLocationId,
                                                       String startAirport, String endAirport,
                                                       String closerStartAirport, String closerEndAirport,
                                                       Status locationFilter, Boolean isStart){
        LOGGER.debug(String.format("/location-options called with locationFilter: %s",
                locationFilter));
        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport,closerStartAirport,closerEndAirport);
        model.addAttribute("isStart",isStart);
        model.addAttribute("optionList",getLocationOptionsList(locationFilter));
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

    private Option buildAirportOptionForSelect(Airport airport, boolean selected) {
        return Option.builder()
                .elementName(airport.getIata())
                .selected(selected)
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

    private List<Option> getLocationOptionsListSelectId(Status status,Integer selectId) {
        return voyagerService.getLocations(status).stream().map(location -> {
            Option option = Option.builder()
                    .elementName(String.format("%s-%s", location.getName(), location.getId()))
                    .display(String.format("%s, %s in %s", location.getName(),
                            location.getSubdivision(), location.getCountryCode()))
                    .value(String.valueOf(location.getId().intValue())).build();
            if (location.getId().equals(selectId)) option.setSelected(true);
            return option;
        }).toList();
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
        model.addAttribute("optionList",optionList);
    }

    private List<Option> addAirportsFromLocation(Location location) {
        List<Option> airportOptionList = new ArrayList<>();
        for (String iata : location.getAirports())
            airportOptionList.add(buildAirportOption(voyagerService.getAirport(iata),true));
        return airportOptionList;
    }

    private void addLocationAttributes(Map<String,Object> mavAttributes,
                                        boolean isStart, Status locationFilter,
                                        Integer locationId, String airportCode,
                                        AirportFilter airportFilter,
                                        AirportFilter closerFilter,
                                        String closerAirportCode) {
        List<Option> airportOptionList = null;
        List<Option> closerAirportOptionList = null;
        if (locationId != 0) {
            Location location = voyagerService.getLocation(locationId);
            List<Airport> airports = getAirports(location, airportFilter);
            airportOptionList = airports.stream()
                    .map(airport -> {
                        if (airport.getIata().equals(airportCode))
                            return buildAirportOptionForSelect(airport, true);
                        return buildAirportOption(airport, true);
                    }).toList();
            if (closerFilter != null) {
                List<Airport> closerAirports = getCloserAirports(location, airports.get(0), closerFilter);
                closerAirportOptionList = closerAirports.stream()
                        .map(airport -> {
                            if (airport.getIata().equals(closerAirportCode))
                                return buildAirportOptionForSelect(airport, true);
                            return buildAirportOptionForSelect(airport);
                        }).toList();
            }
        }
        if (isStart) mavAttributes.put("startLocationId", locationId);
        else mavAttributes.put("endLocationId", locationFilter);
        mavAttributes.put("locationFilter", locationFilter.name());
        mavAttributes.put("optionList", getLocationOptionsListSelectId(locationFilter, locationId));
        mavAttributes.put("airportOptionList", airportOptionList);
        mavAttributes.put("closerAirportOptionList", closerAirportOptionList);
    }

    private void addAirportInputAttributes(Map<String,Object> mavAttributes,
                                           AirportFilter airportFilter,
                                           String airportCode, boolean isStart,
                                           String closerAirportCode) {
        List<Option> airportOptionList = null;
        List<Option> closerAirportOptionList = null;

        String nonDeltaCode = null;
        if (voyagerService.isDeltaIataCode(airportCode)) {
            if (isStart) mavAttributes.put("startAirportInput",airportCode);
            else mavAttributes.put("endAirportInput",airportCode);
        }
        if (voyagerService.isValidIataCode(closerAirportCode)) {
            if (isStart) mavAttributes.put("startAirportInput",closerAirportCode);
            else mavAttributes.put("endAirportInput",closerAirportCode);
            nonDeltaCode = closerAirportCode;
            Airport nonDeltaAirport = voyagerService.getAirport(closerAirportCode);
            AirportFilter selectedFilter = AirportFilter.DELTA;
            List<Airport> airports = getAirportsNear(nonDeltaAirport,selectedFilter);
            airportOptionList = airports.stream().map( airport -> {
                if (airport.getIata().equals(airportCode))
                    return buildAirportOptionForSelect(airport,true);
                return buildAirportOptionForSelect(airport);
            }).toList();
        }
        mavAttributes.put("airportCode",airportCode);
        mavAttributes.put("nonDeltaCode",nonDeltaCode);
        mavAttributes.put("optionList", getAirportOptionsListForInput(airportFilter));
        mavAttributes.put("airportOptionList", airportOptionList);
        mavAttributes.put("closerAirportOptionList", closerAirportOptionList);
    }

    private ModelAndView reverseTrip(boolean isStart, TripFilter tripFilter,
                                     Status locationFilter, Integer locationId,
                                     AirportFilter airportFilter, String airportCode,
                                     AirportFilter closerFilter, String closerAirportCode) {
        Map<String,Object> mavAttributes = new HashMap<>();
        if (locationId != null) addLocationAttributes(mavAttributes,isStart,locationFilter,locationId,airportCode,airportFilter,closerFilter,closerAirportCode);
        else addAirportInputAttributes(mavAttributes,airportFilter,airportCode,isStart,closerAirportCode);
        mavAttributes.put("isStart", isStart);
        mavAttributes.put("tripFilter", tripFilter.name());
        mavAttributes.put("airportFilter", airportFilter.name());
        if (closerFilter != null) mavAttributes.put("closerFilter", closerFilter.name());
        else mavAttributes.put("closerFilter",closerFilter);
        return new ModelAndView("fragments/routes :: trip-input", mavAttributes);
    }

    private Map<String,Object> populateReviewPathAttributes(Integer startLocationId, Integer endLocationId,
                                                            String startAirport, String endAirport,
                                                            String closerStartAirport, String closerEndAirport) {
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

        if (voyagerService.isDeltaIataCode(startAirport))
            reviewPathAttributes.put("startAirport",voyagerService.getAirport(startAirport));
        if (voyagerService.isDeltaIataCode(endAirport))
            reviewPathAttributes.put("endAirport",voyagerService.getAirport(endAirport));
        if (voyagerService.isValidIataCode(closerStartAirport))
            reviewPathAttributes.put("nonDeltaStartAirport",voyagerService.getAirport(closerStartAirport));
        if (voyagerService.isValidIataCode(closerEndAirport))
            reviewPathAttributes.put("nonDeltaEndAirport",voyagerService.getAirport(closerEndAirport));
        return reviewPathAttributes;
    }

    private List<Airport> getAirports(Location location, AirportFilter airportFilter) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        switch (airportFilter) {
            case PINNED -> {
                List<Airport> pinnedAirports = new ArrayList<>();
                for (String iata : location.getAirports())
                    pinnedAirports.add(voyagerService.getAirport(iata));
                return pinnedAirports;
            }
            case PINNED_DELTA -> {
                List<Airport> pinnedAirports = new ArrayList<>();
                for (String iata : location.getAirports()) {
                    if (voyagerService.isDeltaIataCode(iata))
                        pinnedAirports.add(voyagerService.getAirport(iata));
                }
                return pinnedAirports;
            }
            case PINNED_NONDELTA -> {
                List<Airport> pinnedAirports = new ArrayList<>();
                for (String iata : location.getAirports()) {
                    if (!voyagerService.isDeltaIataCode(iata))
                        pinnedAirports.add(voyagerService.getAirport(iata));
                }
                return pinnedAirports;
            }
            case DELTA -> {
                return voyagerService.nearbyAirports(latitude, longitude, 5, Airline.DELTA);
            }
            case CIVIL, ALL -> {
                    return voyagerService.nearbyAirports(latitude, longitude, 5, AirportType.CIVIL);
            }
            case MILITARY -> {
                    return voyagerService.nearbyAirports(latitude, longitude, 5, AirportType.MILITARY);
            }
            default -> {
                LOGGER.error(String.format("getAirports called with airportFilter '%s', not yet implemented",airportFilter.name()));
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("airportFilter '%s' not yet implemented for fetching with location",airportFilter));
            }
        }
    }
    private List<Airport> getAirportsNear(Airport airport, AirportFilter airportFilter) {
        double latitude = airport.getLatitude();
        double longitude = airport.getLongitude();
        switch (airportFilter) {
            case DELTA -> {
                return voyagerService.nearbyAirports(latitude, longitude, 5, Airline.DELTA);
            }
            case CIVIL, ALL -> {
                return voyagerService.nearbyAirports(latitude, longitude, 5, AirportType.CIVIL);
            }
            case MILITARY -> {
                return voyagerService.nearbyAirports(latitude, longitude, 5, AirportType.MILITARY);
            }
            default -> {
                LOGGER.error(String.format("getAirports called with airportFilter '%s', not yet implemented",airportFilter.name()));
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("airportFilter '%s' not yet implemented for fetching with location",airportFilter));
            }
        }
    }
}
