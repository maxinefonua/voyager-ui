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
import org.voyager.validate.ValidationUtils;

import javax.xml.crypto.dsig.spec.XSLTTransformParameterSpec;
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
                                                @RequestParam(name = "airportFilter") List<AirportFilter> airportFilters,
                                                @RequestParam(name = "locationFilter",required = false) List<Status> locationFilters,
                                                TripFilter tripFilterStart, TripFilter tripFilterEnd) {
        ModelAndView reversedPathMav = new ModelAndView(
                buildPath(model,endLocationId,startLocationId,endAirport,startAirport,tripFilterEnd,tripFilterStart),
                model.asMap());

        Map<String,Object> startAttributes = new HashMap<>();
        startAttributes.put("tripFilterStart",tripFilterEnd.name());
        startAttributes.put("airportFilterStart",airportFilters.get(1).name()); // swapped
        if (voyagerService.isValidIataCode(endAirport))
            startAttributes.put("startAirportCode",endAirport);
        if (tripFilterEnd.equals(TripFilter.LOCATION)) {
            startAttributes.put("optionList",getLocationOptionsListSelectId(locationFilters.get(locationFilters.size()-1),endLocationId));
            startAttributes.put("startLocationId", endLocationId);
            startAttributes.put("locationFilter",locationFilters.get(locationFilters.size()-1).name());
            List<Option> airportOptionList = null;
            if (endLocationId != 0) {
                Location location = voyagerService.getLocation(endLocationId);
                List<Airport> airports = getAirports(location,airportFilters.get(1));
                airportOptionList = airports.stream()
                        .map(airport -> {
                            if (airport.getIata().equals(endAirport))
                                return buildAirportOptionForSelect(airport, true);
                            return buildAirportOption(airport, true);
                        }).toList();
            }
            startAttributes.put("airportOptionList",airportOptionList);
        }
        ModelAndView startMav = new ModelAndView(
                "fragments/routes :: trip-input-start",
                startAttributes);

        Map<String,Object> endAttributes = new HashMap<>();
        endAttributes.put("tripFilterEnd",tripFilterStart.name());
        endAttributes.put("airportFilterEnd",airportFilters.get(0).name()); // swapped
        if (voyagerService.isValidIataCode(startAirport))
            endAttributes.put("endAirportCode",startAirport);
        if (tripFilterStart.equals(TripFilter.LOCATION)) {
            endAttributes.put("optionList",getLocationOptionsListSelectId(locationFilters.get(0),startLocationId));
            endAttributes.put("endLocationId", startLocationId);
            endAttributes.put("locationFilter",locationFilters.get(0).name());
            List<Option> airportOptionList = null;
            if (startLocationId != 0) { // selection was made
                Location location = voyagerService.getLocation(startLocationId);
                List<Airport> airports = getAirports(location,airportFilters.get(0));
                airportOptionList = airports.stream()
                        .map(airport -> {
                            if (airport.getIata().equals(startAirport))
                                return buildAirportOptionForSelect(airport, true);
                            return buildAirportOption(airport, true);
                        }).toList();
            }
            endAttributes.put("airportOptionList",airportOptionList);
        }
        ModelAndView endMav = new ModelAndView(
                "fragments/routes :: trip-input-end",
                endAttributes);
        return List.of(reversedPathMav,startMav,endMav);
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
        if (voyagerService.isDeltaIataCode(startAirport.toUpperCase()))
            model.addAttribute("startAirport",voyagerService.getAirport(startAirport.toUpperCase()));
        else if (voyagerService.isValidIataCode(startAirport.toUpperCase()))
            model.addAttribute("nonDeltaStartAirport",voyagerService.getAirport(startAirport.toUpperCase()));
        if (voyagerService.isDeltaIataCode(endAirport.toUpperCase()))
            model.addAttribute("endAirport",voyagerService.getAirport(endAirport.toUpperCase()));
        else if (voyagerService.isValidIataCode(endAirport.toUpperCase()))
            model.addAttribute("nonDeltaEndAirport",voyagerService.getAirport(endAirport.toUpperCase()));
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

    @GetMapping("/trips/location-selected")
    public Collection<ModelAndView> locationSelected(Model model,
                                                   Integer startLocationId, Integer endLocationId,
                                                   String startAirport, String endAirport,
                                                   @RequestParam Boolean isStart,
                                                   @RequestParam(AIRPORT_FILTER_PARAM_NAME) AirportFilter airportFilter) {
        LOGGER.debug("nearbyAirports called with startLocationId: "+ startLocationId + ", endLocationId: " + endLocationId);

        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport);

        if ((isStart && startLocationId != 0) || (!isStart && endLocationId != 0)) {
            Location location = null;
            if (isStart) location = voyagerService.getLocation(startLocationId);
            else location = voyagerService.getLocation(endLocationId);

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            List<Airport> nearbyAirports = new ArrayList<>();
            switch (airportFilter) {
                case PINNED_DELTA -> {
                    for (String iata : location.getAirports()) {
                        if (voyagerService.isDeltaIataCode(iata))
                            nearbyAirports.add(voyagerService.getAirport(iata));
                    }
                }
                case PINNED_NONDELTA -> {
                    for (String iata : location.getAirports()) {
                        if (!voyagerService.isDeltaIataCode(iata))
                            nearbyAirports.add(voyagerService.getAirport(iata));
                    }
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
            List<Option> optionList = nearbyAirports.stream().map(airport -> buildAirportOption(airport, true)).toList();
            model.addAttribute("optionList", optionList);
        }
        model.addAttribute("isStart",isStart);
        return List.of(new ModelAndView("fragments/routes :: airport-select-options-for-location",model.asMap()),
                new ModelAndView("fragments/routes :: review-path",reviewPathAttributes));
    }

    @GetMapping("/trips/nearby-airports-location")
    public Collection<ModelAndView> nearbyAirports(Model model,
                                 Integer startLocationId, Integer endLocationId,
                                 String startAirport, String endAirport,
                                 @RequestParam Boolean isStart,
                                 @RequestParam(AIRPORT_FILTER_PARAM_NAME) AirportFilter airportFilter) {
        LOGGER.debug("nearbyAirports called with startLocationId: "+ startLocationId + ", endLocationId: " + endLocationId);

        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport);

        if ((isStart && startLocationId != 0) || (!isStart && endLocationId != 0)) {
            Location location = null;
            if (isStart) location = voyagerService.getLocation(startLocationId);
            else location = voyagerService.getLocation(endLocationId);

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            List<Airport> nearbyAirports = new ArrayList<>();
            switch (airportFilter) {
                case PINNED_DELTA -> {
                    for (String iata : location.getAirports()) {
                        if (voyagerService.isDeltaIataCode(iata))
                            nearbyAirports.add(voyagerService.getAirport(iata));
                    }
                }
                case PINNED_NONDELTA -> {
                    for (String iata : location.getAirports()) {
                        if (!voyagerService.isDeltaIataCode(iata))
                            nearbyAirports.add(voyagerService.getAirport(iata));
                    }
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
                                                      AirportFilter airportFilter,
                                                      Boolean isStart){
        LOGGER.debug(String.format("/airport-options called with airportFilter %s",
                airportFilter.name()));
        List<Option> optionList = new ArrayList<>();
        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport);
        optionList.addAll(getAirportOptionsListForInput(airportFilter));
        model.addAttribute("isStart",isStart);
        model.addAttribute("optionList",optionList);
        return List.of(new ModelAndView("fragments/options :: trip-select-options",model.asMap()),
                new ModelAndView("fragments/routes :: review-path",reviewPathAttributes));
    }

    @GetMapping("/location-options")
    public Collection<ModelAndView> getLocationOptions(Model model,
                                                       Integer startLocationId, Integer endLocationId,
                                                       String startAirport, String endAirport,
                                                       Status locationFilter, Boolean isStart){
        LOGGER.debug(String.format("/location-options called with locationFilter: %s",
                locationFilter));
        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport);

        List<Location> locationList = voyagerService.getLocations(locationFilter);
        List<Option> airportOptionList = new ArrayList<>();
        if (!locationList.isEmpty()) {
            Location first = locationList.get(0);
            first.getAirports().forEach(iata -> {
                airportOptionList.add(buildAirportOption(voyagerService.getAirport(iata),true));
            });
            if (isStart) {
                reviewPathAttributes.put("startLocation", first);
                reviewPathAttributes.remove("startAirport");
            } else {
                reviewPathAttributes.put("endLocation",first);
                reviewPathAttributes.remove("endAirport");
            }
        }
        model.addAttribute("isStart",isStart);
        model.addAttribute("optionList",getLocationOptionsList(locationFilter));
        model.addAttribute("airportOptionList",airportOptionList);
        return List.of(new ModelAndView("fragments/options :: trip-select-options",model.asMap()),
                new ModelAndView("fragments/routes :: review-path",reviewPathAttributes));
    }

    @GetMapping("/trip-options")
    public Collection<ModelAndView> getTripOptions(Model model,
                                                   @RequestParam(name = "tripFilterStart") List<TripFilter> tripFilterStartList,
                                                   @RequestParam(name = "tripFilterEnd") TripFilter tripFilterEndList,
                                                   Integer startLocationId, Integer endLocationId,
                                                   String startAirport, String endAirport,
                                                   String locationFilter, Boolean isStart){
        TripFilter selection = null;
        if (isStart) selection = tripFilterStartList.get(0);
        else selection = tripFilterStartList.get(1);
        LOGGER.debug(String.format("/trip-options called with tripfilter selection: %s, locationFilter %s",
                selection.name(),locationFilter));
        List<Option> optionList = new ArrayList<>();
        Map<String,Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId,endLocationId,
                startAirport,endAirport);

        switch (selection) {
            case LOCATION -> {
                Status status = ValidationUtils.getLocationStatusElseDefault(locationFilter);
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
                AirportFilter airportFilter = ValidationUtils.getAirportFilterElseDefault(locationFilter);
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

}
