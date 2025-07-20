package org.voyager.controller;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.voyager.model.*;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.flight.Flight;
import org.voyager.model.location.*;
import org.voyager.model.response.SearchResult;
import org.voyager.model.result.ResultSearch;
import org.voyager.model.result.ResultSearchFull;
import org.voyager.model.route.PathAirline;
import org.voyager.model.route.PathResponse;
import org.voyager.model.route.Route;
import org.voyager.service.*;
import org.voyager.utils.LocationMapperUtils;

import java.time.*;
import java.util.*;

import static org.voyager.utils.ConstantsUI.AIRPORT_FILTER_PARAM_NAME;
import static org.voyager.utils.ConstantsUtils.SOURCE_ID_PARAM_NAME;

@Controller
public class TripsController {
    private static final TripFilter DEFAULT_TRIP_FILTER = TripFilter.AIRPORT;
    private static final Status DEFAULT_LOCATION_STATUS_FILTER = Status.SAVED;
    private static final AirportFilter DEFAULT_AIRPORT_FILTER = AirportFilter.CIVIL;
    private static final AirportFilter DEFAULT_CLOSER_FILTER = AirportFilter.CIVIL;
    private static final Logger LOGGER = LoggerFactory.getLogger(TripsController.class);
    private Stack<Location> recentLocations = new Stack<>();


    @Autowired
    private VoyagerService voyagerService;

    void addDefaultAttributes(Model model) {
        model.addAttribute("tripFilterStart", DEFAULT_TRIP_FILTER.name());
        model.addAttribute("tripFilterEnd", DEFAULT_TRIP_FILTER.name());
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

    @GetMapping("/exclude-route")
    public String excludeRoute(Model model, String startAirport, String endAirport,
                               String closerStartAirport, String closerEndAirport,
                               Integer routeId, PathExclusions pathExclusions) {
        Map<String, Object> routeAttributes = new HashMap<>();
        pathExclusions.getRouteIds().add(routeId);
        populateDeltaRoute(routeAttributes, startAirport, endAirport, closerStartAirport, closerEndAirport, pathExclusions);
        model.addAllAttributes(routeAttributes);
        return "fragments/trips :: delta-route";
    }

    @GetMapping("/enable-route")
    public String enableRoute(Model model, String startAirport, String endAirport,
                              String closerStartAirport, String closerEndAirport,
                              Integer routeId, PathExclusions pathExclusions) {
        Map<String, Object> routeAttributes = new HashMap<>();
        pathExclusions.getRouteIds().remove(routeId);
        populateDeltaRoute(routeAttributes, startAirport, endAirport, closerStartAirport,closerEndAirport, pathExclusions);
        model.addAllAttributes(routeAttributes);
        return "fragments/trips :: delta-route";
    }

    @GetMapping("/build-path")
    public String buildPath(Model model, @RequestParam(name = SOURCE_ID_PARAM_NAME) String[] sourceIds,
                            PathExclusions pathExclusions) {
        Source source = Source.valueOf(voyagerService.lookupAttribution().getName().toUpperCase());
        Location startLocation = null;
        Location endLocation = null;
        List<String> startAirportCodes = List.of();
        List<String> endAirportCodes = List.of();
        if (StringUtils.isNotBlank(sourceIds[0])) startLocation = voyagerService.getLocation(source,sourceIds[0]);
        if (StringUtils.isNotBlank(sourceIds[1])) endLocation = voyagerService.getLocation(source,sourceIds[1]);
        if (startLocation != null && endLocation != null) {
            startAirportCodes = startLocation.getAirports();
            endAirportCodes = endLocation.getAirports();
            if (!startAirportCodes.isEmpty() && !endAirportCodes.isEmpty()) {
                List<Airline> airlineListStart = voyagerService.getAirlines(startAirportCodes);
                List<Airline> airlineListEnd = voyagerService.getAirlines(endAirportCodes);
                List<Option> airlineOptionList = Arrays.stream(Airline.values())
                        .sorted(Comparator.comparing(Airline::getDisplayText))
                        .map(airline -> buildAirlineOptionForSelectFiltered(airline,
                                (!airlineListStart.contains(airline) || !airlineListEnd.contains(airline))))
                        .toList();
                model.addAttribute("airlineOptionList", airlineOptionList);
            }
        }
        model.addAttribute("startLocation",startLocation);
        model.addAttribute("endLocation",endLocation);
        model.addAttribute("startAirportCodes",startAirportCodes);
        model.addAttribute("endAirportCodes",endAirportCodes);
        return "fragments/trips :: display-path";
    }

    @GetMapping("/path")
    public String path(Model model, Integer startLocationId, Integer endLocationId,
                       PathExclusions pathExclusions, String airlineSelection) {
        Location startLocation = voyagerService.getLocation(startLocationId);
        Location endLocation = voyagerService.getLocation(endLocationId);
        List<String> originList = startLocation.getAirports();
        List<String> destinationList = endLocation.getAirports();
        if (!originList.isEmpty() && !destinationList.isEmpty()) {
            PathResponse<PathAirline> pathResponse;
            if (airlineSelection.equals("ALL"))
                pathResponse = voyagerService.getPath(originList, destinationList,
                        pathExclusions.getAirports(), pathExclusions.getRouteIds());
            else pathResponse = voyagerService.getPath(originList, destinationList,
                    pathExclusions.getAirports(), pathExclusions.getRouteIds(), Airline.valueOf(airlineSelection));
            List<PathAirline> pathAirlineList = pathResponse.getResponseList();
            List<List<Airport>> pathAirportsList = new ArrayList<>();
            if (!pathAirlineList.isEmpty()) {
                for (PathAirline pathAirline : pathAirlineList) {
                    List<Airport> pathAirports = new ArrayList<>();
                    pathAirports.add(voyagerService.getAirport(pathAirline.getRouteList().get(0).getOrigin()));
                    for (Route route : pathAirline.getRouteList()) {
                        pathAirports.add(voyagerService.getAirport(route.getDestination()));
                    }
                    pathAirportsList.add(pathAirports);
                }
            }
            model.addAttribute("pathAirlineList",pathAirlineList);
            model.addAttribute("pathAirportsList",pathAirportsList);
        }
        return "fragments/trips :: path-airline";
    }

    @GetMapping("/radio-selection")
    public Collection<ModelAndView> selectFrom(Model model,
                                               TripFilter tripFilterStart, TripFilter tripFilterEnd,
                                               Integer startLocationId, Integer endLocationId,
                                               String startAirport, String endAirport,
                                               String closerStartAirport, String closerEndAirport,
                                               @RequestParam Boolean isStart) {
        TripFilter tripFilter = isStart ? tripFilterStart : tripFilterEnd;
        model.addAttribute("selection", tripFilter.name());
        model.addAttribute("isStart", isStart);
        Map<String, Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId, endLocationId,
                startAirport, endAirport, closerStartAirport, closerEndAirport);
        if (isStart) {
            reviewPathAttributes.put("tripFilterStart", tripFilter.name());
            reviewPathAttributes.remove("startLocation");
            reviewPathAttributes.remove("startAirport");
        } else {
            reviewPathAttributes.put("tripFilterEnd", tripFilter.name());
            reviewPathAttributes.remove("endLocation");
            reviewPathAttributes.remove("endAirport");
        }

        switch (tripFilter) {
            case LOCATION -> {
                populateLocationDefaults(model);
                return List.of(new ModelAndView("fragments/trips :: by-location", model.asMap()),
                        new ModelAndView("fragments/trips :: review-path", reviewPathAttributes));
            }
            case AIRPORT -> {
                model.addAttribute("isStart", isStart);
                model.addAttribute("optionList", getDefaultAirportOptionListForInput());
                return List.of(new ModelAndView("fragments/trips :: by-airport", model.asMap()),
                        new ModelAndView("fragments/trips :: review-path", reviewPathAttributes));
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
        model.addAttribute("locations", locations);
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
                                                 PathExclusions pathExclusions,
                                                 @RequestParam Boolean isStart) {
        LOGGER.debug("airportInput called with startAirportCode: " + startAirportCode + ", endAirportCode: " + endAirportCode);
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
                    nonDeltaAirport.getLongitude(), 10, Airline.DELTA);
            airportOptionList = nearbyDelta.stream().map(this::buildAirportOptionForSelect).toList();
        }

        model.addAttribute("airportCode", airportCode);
        model.addAttribute("nonDeltaCode", nonDeltaCode);
        model.addAttribute("isStart", isStart);
        model.addAttribute("airportOptionList", airportOptionList);
        model.addAttribute("closerFilter", closerFilter);

        Map<String, Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId, endLocationId,
                startAirport, endAirport, closerStartAirport, closerEndAirport);
        populateDeltaRoute(reviewPathAttributes, startAirport, endAirport,closerStartAirport,closerEndAirport, pathExclusions);

        return List.of(
                new ModelAndView("fragments/trips :: update-airport-input", model.asMap()),
                new ModelAndView("fragments/trips :: review-path", reviewPathAttributes)
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
        LOGGER.debug("nearbyAirports called with startLocationId: " + startLocationId + ", endLocationId: " + endLocationId);
        Map<String, Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId, endLocationId,
                startAirport, endAirport, closerStartAirport, closerEndAirport);
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
            List<Airport> nearbyAirports = getAirports(location, airportFilter);
            List<Option> optionList = nearbyAirports.stream().map(this::buildAirportOptionForSelect).toList();
            model.addAttribute("optionList", optionList);
            if (airportFilter == AirportFilter.PINNED || airportFilter == AirportFilter.PINNED_DELTA
                    || airportFilter == AirportFilter.PINNED_NONDELTA) {
                for (Airport pinned : nearbyAirports) {
                    pinned.setDistance(Airport.calculateDistanceKm(location.getLatitude(), location.getLongitude(),
                            pinned.getLatitude(), pinned.getLongitude()));
                }
            }
            List<Airport> closerAirports = getCloserAirports(location, nearbyAirports.get(0), DEFAULT_CLOSER_FILTER);
            if (!closerAirports.isEmpty()) {
                closerAirportOptionList = closerAirports.stream().limit(10)
                        .map(this::buildAirportOptionForSelect).toList();
            }
        }
        Map<String, Object> closerAttributes = new HashMap<>();
        closerAttributes.put("closerAirportOptionList", closerAirportOptionList);
        if (closerFilter != null) closerAttributes.put("closerFilter", closerFilter.name());
        else closerAttributes.put("closerFilter", closerFilter);
        closerAttributes.put("isStart", isStart);

        model.addAttribute("isStart", isStart);
        return List.of(
                new ModelAndView("fragments/trips :: airport-select-options-for-location", model.asMap()),
                new ModelAndView("fragments/trips :: review-path", reviewPathAttributes),
                new ModelAndView("fragments/trips :: closer-airports", closerAttributes)
        );
    }

    private List<Airport> getCloserAirports(Location location, Airport closest, AirportFilter closerFilter) {
        List<Airport> airports = getAirports(location, closerFilter);
        List<Airport> closerAirports = new ArrayList<>();

        double minDistance = Airport.calculateDistanceKm(location.getLatitude(), location.getLongitude(),
                closest.getLatitude(), closest.getLongitude());

        for (Airport airport : airports) {
            if (airport.getDistance() == null || closest.getDistance() == null) {
                double distance = Airport.calculateDistanceKm(location.getLatitude(), location.getLongitude(),
                        airport.getLatitude(), airport.getLongitude());
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
        LOGGER.debug("closerAirports called with airportFilter: " + airportFilter);
        Map<String, Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId, endLocationId,
                startAirport, endAirport, closerStartAirport, closerEndAirport);
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
            Airport closestAirport = getAirports(location, airportFilter).get(0);
            AirportFilter closerFilter = isStart ? closerFilterStart : closerFilterEnd;
            List<Airport> airports = getCloserAirports(location, closestAirport, closerFilter);
            List<Option> closerAirportOptionList = airports.stream().map(this::buildAirportOptionForSelect).toList();
            model.addAttribute("closerAirportOptionList", closerAirportOptionList);
        }
        model.addAttribute("isStart", isStart);
        return List.of(new ModelAndView("fragments/trips :: closer-airport-options-for-location", model.asMap()),
                new ModelAndView("fragments/trips :: review-path", reviewPathAttributes));
    }

    @GetMapping("/lookup")
    public String lookup(Model model, String inputText)  {
        List<Option> optionList = new ArrayList<>();
        SearchResult<ResultDetails> voyagerResponse = voyagerService.lookupWithDetails(inputText, 0, 10);
        List<ResultDetails> lookupResults = voyagerResponse.getResults();
        Integer totalResultsCount = voyagerResponse.getResultCount();
        optionList.addAll(lookupResults.stream().map(resultDetails -> {
            ResultSearch resultSearch = resultDetails.getResultSearch();
            String displayText = String.format("%s, %s | %s",resultSearch.getName(),
                    resultSearch.getSubdivision(),resultSearch.getCountryName());
            String valueText = resultSearch.getSourceId();
            return Option.builder().display(displayText).value(valueText).build();
        }).toList());
        model.addAttribute("optionList",optionList);
        model.addAttribute("tripFilter",TripFilter.LOCATION);
        return "fragments/options :: trip-input-options";
    }


    @GetMapping("/trips/nearby-airports-location")
    public Collection<ModelAndView> nearbyAirports(Model model,
                                                   Integer startLocationId, Integer endLocationId,
                                                   String startAirport, String endAirport,
                                                   String closerStartAirport, String closerEndAirport,
                                                   @RequestParam Boolean isStart,
                                                   @RequestParam(AIRPORT_FILTER_PARAM_NAME) AirportFilter airportFilter) {
        LOGGER.debug("nearbyAirports called with startLocationId: " + startLocationId + ", endLocationId: " + endLocationId);

        Map<String, Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId, endLocationId,
                startAirport, endAirport, closerStartAirport, closerEndAirport);

        if ((isStart && startLocationId != 0) || (!isStart && endLocationId != 0)) {
            Location location = null;
            if (isStart) location = voyagerService.getLocation(startLocationId);
            else location = voyagerService.getLocation(endLocationId);

            List<Airport> nearbyAirports = getAirports(location, airportFilter);
            List<Option> optionList = nearbyAirports.stream().map(this::buildAirportOptionForSelect).toList();
            model.addAttribute("optionList", optionList);
        }
        model.addAttribute("isStart", isStart);
        return List.of(new ModelAndView("fragments/trips :: airport-select-options-for-location", model.asMap()),
                new ModelAndView("fragments/trips :: review-path", reviewPathAttributes));
    }

    @GetMapping("/airport-options")
    public Collection<ModelAndView> getAirportOptions(Model model,
                                                      Integer startLocationId, Integer endLocationId,
                                                      String startAirport, String endAirport,
                                                      String closerStartAirport, String closerEndAirport,
                                                      AirportFilter airportFilter,
                                                      Boolean isStart) {
        LOGGER.debug(String.format("/airport-options called with airportFilter %s",
                airportFilter.name()));
        List<Option> optionList = getAirportOptionsListForInput(airportFilter);
        model.addAttribute("isStart", isStart);
        model.addAttribute("optionList", optionList);

        Map<String, Object> airportInputAttributes = new HashMap<>();
        airportInputAttributes.put("isStart", isStart);

        Map<String, Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId, endLocationId,
                startAirport, endAirport, closerStartAirport, closerEndAirport);

        return List.of(new ModelAndView("fragments/options :: trip-select-options", model.asMap()),
                new ModelAndView("fragments/trips :: review-path", reviewPathAttributes),
                new ModelAndView("fragments/trips :: update-airport-input", airportInputAttributes));
    }

    @GetMapping("/trip-filter-options")
    public Collection<ModelAndView> getTripFilterOptions(Model model,
                                                      Integer startLocationId, Integer endLocationId,
                                                      String startAirport, String endAirport,
                                                      String closerStartAirport, String closerEndAirport,
                                                         @NotNull TripFilter tripFilter,
                                                      Boolean isStart) {
        LOGGER.debug(String.format("/trip-filter-options called with tripFilter %s",
                tripFilter.name()));
        List<Option> optionList = new ArrayList<>();
        if (tripFilter.equals(TripFilter.AIRPORT))
            optionList.addAll(getAirportOptionsListForInput(AirportFilter.CIVIL));
        if (tripFilter.equals(TripFilter.LOCATION))
            optionList.addAll(getLocationOptionsList());
        model.addAttribute("tripFilter",tripFilter.name());
        model.addAttribute("isStart", isStart);
        model.addAttribute("optionList", optionList);


        ModelAndView reviewMap = buildUpdatedReview(isStart,tripFilter,null);
        return List.of(new ModelAndView("fragments/options :: trip-input-options", model.asMap()),
                reviewMap);
    }

    @GetMapping("/update-review")
    public Collection<ModelAndView> updateReview(Boolean isStart, String sourceId) {
        Location location = null;
        if (StringUtils.isNotBlank(sourceId)) {
            Source source = Source.valueOf(voyagerService.lookupAttribution().getName().toUpperCase());
            try {
                location = voyagerService.getLocation(source, sourceId);
            } catch (ResponseStatusException e) {
                ResultSearchFull resultSearchFull = voyagerService.getResultSearchFull(sourceId);
                LocationForm locationForm = LocationMapperUtils.toLocationForm(resultSearchFull);
                location = voyagerService.addLocation(locationForm);
            }
            recentLocations.remove(location);
            recentLocations.push(location);
        }
        ModelAndView airportsMap = buildPinAirports(isStart,location,null);
        ModelAndView reviewMap = buildUpdatedReview(isStart,TripFilter.LOCATION,location);
        return List.of(reviewMap,airportsMap);
    }

    @GetMapping("/location-options")
    public Collection<ModelAndView> getLocationOptions(Model model,
                                                       Integer startLocationId, Integer endLocationId,
                                                       String startAirport, String endAirport,
                                                       String closerStartAirport, String closerEndAirport,
                                                       Status locationFilter, Boolean isStart) {
        LOGGER.debug(String.format("/location-options called with locationFilter: %s",
                locationFilter));
        Map<String, Object> reviewPathAttributes = populateReviewPathAttributes(startLocationId, endLocationId,
                startAirport, endAirport, closerStartAirport, closerEndAirport);
        model.addAttribute("isStart", isStart);
        model.addAttribute("optionList", getLocationOptionsList(locationFilter));
        return List.of(new ModelAndView("fragments/options :: trip-select-options", model.asMap()),
                new ModelAndView("fragments/trips :: review-path", reviewPathAttributes));
    }

    @GetMapping("/airline-options")
    public String airlineOptions(Model model) {
        List<Option> optionList = Arrays.stream(Airline.values()).map(this::buildAirlineOptionForSelect).toList();
        model.addAttribute("optionList", optionList);
        return "fragments/options :: trip-select-options";
    }

    @GetMapping("/add-airport-code")
    public String addAirportCode(Model model, Boolean isStart, @ModelAttribute AirportCodes airportCodes,
                                 String airportCode,String sourceId) {
        if (voyagerService.isValidIataCode(airportCode) && !airportCodes.getCodes().contains(airportCode))
            airportCodes.getCodes().add(airportCode);
        Source source = Source.valueOf(voyagerService.lookupAttribution().getName().toUpperCase());
        Location location = voyagerService.getLocation(source,sourceId);
        LocationPatch locationPatch = LocationPatch.builder().airports(airportCodes.getCodes()).build();
        voyagerService.patchLocation(location.getId(),locationPatch);
        model.addAttribute("airportCodes",airportCodes);
        model.addAttribute("isStart",isStart);
        return "fragments/trips :: airport-codes";
    }

    @GetMapping("/remove-airport-code")
    public String removeAirportCode(Model model, Boolean isStart, @ModelAttribute AirportCodes airportCodes,
                                    String airportCode, String sourceId) {
        airportCodes.getCodes().remove(airportCode);
        Source source = Source.valueOf(voyagerService.lookupAttribution().getName().toUpperCase());
        Location location = voyagerService.getLocation(source,sourceId);
        LocationPatch locationPatch = LocationPatch.builder().airports(airportCodes.getCodes()).build();
        voyagerService.patchLocation(location.getId(),locationPatch);
        model.addAttribute("airportCodes",airportCodes);
        model.addAttribute("isStart",isStart);
        return "fragments/trips :: airport-codes";
    }

    private List<Option> getAirportOptionsListForInput(AirportFilter airportFilter) {
        List<Airport> airportList = new ArrayList<>();
        switch (airportFilter) {
            case DELTA -> airportList.addAll(voyagerService.airports(Airline.DELTA));
            case CIVIL -> airportList.addAll(voyagerService.airports(AirportType.CIVIL));
            case MILITARY -> airportList.addAll(voyagerService.airports(AirportType.MILITARY));
            case ALL ->
                    airportList.addAll(voyagerService.airports(Arrays.asList(AirportType.CIVIL, AirportType.MILITARY)));
        }
        return airportList.stream().map(this::buildAirportOption).toList();
    }

    private Option buildAirportOptionForInput(Airport airport) {
        return Option.builder()
                .elementName(airport.getIata())
                .name(airport.getName())
                .city(airport.getCity())
                .subdivision(airport.getSubdivision())
                .country(airport.getCountryCode())
                .longitude(airport.getLongitude())
                .latitude(airport.getLatitude())
                .display(String.format("%s | %s, %s of %s", airport.getName(), airport.getCity(),
                        airport.getSubdivision(), airport.getCountryCode()))
                .value(airport.getIata()).build();
    }

    private Option buildAirportOption(Airport airport) {
        return Option.builder()
                .elementName(airport.getIata())
                .display(String.format("%s | %s, %s of %s", airport.getName(), airport.getCity(),
                        airport.getSubdivision(), airport.getCountryCode()))
                .value(airport.getIata()).build();
    }

    private Option buildAirportOptionForSelect(Airport airport) {
        return Option.builder()
                .elementName(airport.getIata())
                .display(String.format("%s | %s", airport.getIata(), airport.getName()))
                .value(airport.getIata()).build();
    }

    private Option buildAirlineOptionForSelect(Airline airline) {
        return Option.builder()
                .display(airline.getDisplayText())
                .value(airline.name()).build();
    }

    private Option buildAirlineOptionForSelectFiltered(Airline airline,boolean disabled) {
        return Option.builder()
                .display(airline.getDisplayText())
                .disabled(disabled)
                .value(airline.name()).build();
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
                        .elementName(String.format("%s-%s", location.getName(), location.getId()))
                        .display(String.format("%s, %s in %s", location.getName(),
                                location.getSubdivision(), location.getCountryCode()))
                        .value(String.valueOf(location.getId().intValue())).build())
                .toList();
    }

    private List<Option> getAllLocationOptionsList() {
        return voyagerService.getLocations().stream().map(location -> Option.builder()
                        .elementName(String.format("%s-%s", location.getName(), location.getId()))
                        .display(String.format("%s, %s in %s", location.getName(),
                                location.getSubdivision(), location.getCountryCode()))
                        .value(String.valueOf(location.getId().intValue())).build())
                .toList();
    }

    private List<Option> getLatestLocationsOptionsList() {
        return voyagerService.getLocations().stream().map(location -> Option.builder()
                        .elementName(String.format("%s-%s", location.getName(), location.getId()))
                        .display(String.format("%s, %s in %s", location.getName(),
                                location.getSubdivision(), location.getCountryCode()))
                        .value(String.valueOf(location.getId().intValue())).build())
                .toList();
    }

    private List<Option> getLocationOptionsList() {
        if (recentLocations.isEmpty())
            recentLocations.addAll(voyagerService.getLocations().stream().limit(10).toList());
        return recentLocations.stream().map(location -> Option.builder()
                        .elementName(String.format("%s-%s", location.getName(), location.getId()))
                        .display(String.format("%s, %s | %s", location.getName(),
                                location.getSubdivision(), location.getCountryCode()))
                        .value(location.getSourceId()).build())
                .toList();
    }

    private List<Option> getLocationOptionsListSelectId(Status status, Integer selectId) {
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
                .map(airport -> buildAirportOption(airport))
                .toList();
    }


    private void populateLocationDefaults(Model model) {
        recentLocations.clear();
        recentLocations.addAll(voyagerService.getLocations().stream().limit(10).toList());
        List<Option> optionList = recentLocations.stream()
                .map(location -> Option.builder().elementName(String.format("%s-%s",
                                location.getName(), location.getId())).display(String.format("%s, %s in %s",
                                location.getName(), location.getSubdivision(), location.getCountryCode()))
                        .value(String.valueOf(location.getId().intValue())).build())
                .toList();
        model.addAttribute("optionList", optionList);
    }

    private List<Option> addAirportsFromLocation(Location location) {
        List<Option> airportOptionList = new ArrayList<>();
        for (String iata : location.getAirports())
            airportOptionList.add(buildAirportOptionForSelect(voyagerService.getAirport(iata)));
        return airportOptionList;
    }

    private void addLocationAttributes(Map<String, Object> mavAttributes,
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
                        return buildAirportOptionForSelect(airport);
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

    private void addAirportInputAttributes(Map<String, Object> mavAttributes,
                                           AirportFilter airportFilter,
                                           String airportCode, boolean isStart,
                                           String closerAirportCode) {
        List<Option> airportOptionList = null;
        List<Option> closerAirportOptionList = null;

        String nonDeltaCode = null;
        if (voyagerService.isDeltaIataCode(airportCode)) {
            if (isStart) mavAttributes.put("startAirportInput", airportCode);
            else mavAttributes.put("endAirportInput", airportCode);
        }
        if (voyagerService.isValidIataCode(closerAirportCode)) {
            if (isStart) mavAttributes.put("startAirportInput", closerAirportCode);
            else mavAttributes.put("endAirportInput", closerAirportCode);
            nonDeltaCode = closerAirportCode;
            Airport nonDeltaAirport = voyagerService.getAirport(closerAirportCode);
            AirportFilter selectedFilter = AirportFilter.DELTA;
            List<Airport> airports = getAirportsNear(nonDeltaAirport, selectedFilter);
            airportOptionList = airports.stream().map(airport -> {
                if (airport.getIata().equals(airportCode))
                    return buildAirportOptionForSelect(airport, true);
                return buildAirportOptionForSelect(airport);
            }).toList();
        }
        mavAttributes.put("airportCode", airportCode);
        mavAttributes.put("nonDeltaCode", nonDeltaCode);
        mavAttributes.put("optionList", getAirportOptionsListForInput(airportFilter));
        mavAttributes.put("airportOptionList", airportOptionList);
        mavAttributes.put("closerAirportOptionList", closerAirportOptionList);
    }

    private ModelAndView reverseTrip(boolean isStart, TripFilter tripFilter,
                                     Status locationFilter, Integer locationId,
                                     AirportFilter airportFilter, String airportCode,
                                     AirportFilter closerFilter, String closerAirportCode) {
        Map<String, Object> mavAttributes = new HashMap<>();
        if (locationId != null)
            addLocationAttributes(mavAttributes, isStart, locationFilter, locationId, airportCode, airportFilter, closerFilter, closerAirportCode);
        else addAirportInputAttributes(mavAttributes, airportFilter, airportCode, isStart, closerAirportCode);
        mavAttributes.put("isStart", isStart);
        mavAttributes.put("tripFilter", tripFilter.name());
        mavAttributes.put("airportFilter", airportFilter.name());
        if (closerFilter != null) mavAttributes.put("closerFilter", closerFilter.name());
        else mavAttributes.put("closerFilter", closerFilter);
        return new ModelAndView("fragments/trips :: trip-input", mavAttributes);
    }

    private Map<String, Object> populateReviewPathAttributes(Integer startLocationId, Integer endLocationId,
                                                             String startAirport, String endAirport,
                                                             String closerStartAirport, String closerEndAirport) {
        Map<String, Object> reviewPathAttributes = new HashMap<>();
        if (startLocationId == null) {
            reviewPathAttributes.put("tripFilterStart", TripFilter.AIRPORT.name());
        } else {
            reviewPathAttributes.put("tripFilterStart", TripFilter.LOCATION.name());
            if (startLocationId != 0)
                reviewPathAttributes.put("startLocation", voyagerService.getLocation(startLocationId));
        }

        if (endLocationId == null) {
            reviewPathAttributes.put("tripFilterEnd", TripFilter.AIRPORT.name());
        } else {
            reviewPathAttributes.put("tripFilterEnd", TripFilter.LOCATION.name());
            if (endLocationId != 0)
                reviewPathAttributes.put("endLocation", voyagerService.getLocation(endLocationId));
        }

        Airport deltaStart = null;
        Airport deltaEnd = null;

        if (voyagerService.isDeltaIataCode(startAirport)) {
            deltaStart = voyagerService.getAirport(startAirport);
            reviewPathAttributes.put("startAirport", deltaStart);
        }
        if (voyagerService.isDeltaIataCode(endAirport)) {
            deltaEnd = voyagerService.getAirport(endAirport);
            reviewPathAttributes.put("endAirport", deltaEnd);
        }
        if (voyagerService.isValidIataCode(closerStartAirport))
            reviewPathAttributes.put("nonDeltaStartAirport", voyagerService.getAirport(closerStartAirport));
        if (voyagerService.isValidIataCode(closerEndAirport))
            reviewPathAttributes.put("nonDeltaEndAirport", voyagerService.getAirport(closerEndAirport));
        return reviewPathAttributes;
    }

    private ModelAndView buildUpdatedReview(Boolean isStart, TripFilter tripFilter,Location location) {
        Map<String, Object> reviewAttributes = new HashMap<>();
        reviewAttributes.put("tripFilter",tripFilter.name());
        reviewAttributes.put("isStart",isStart);
        reviewAttributes.put("location",location);
        List<Option> nearbyAirportOptionList = null;
        if (location != null) {
            Double longitude = location.getLongitude();
            Double latitude = location.getLatitude();
            List<Airport> nearbyAirportList = voyagerService.nearbyAirports(latitude,longitude,5,Airline.DELTA);
            nearbyAirportOptionList = nearbyAirportList.stream().map(this::buildAirportOptionForSelect).toList();
        }
        reviewAttributes.put("nearbyAirportOptionList",nearbyAirportOptionList);
        return new ModelAndView("fragments/trips :: display-review",reviewAttributes);
    }

    private ModelAndView buildPinAirports(Boolean isStart, Location location, Airline airline) {
        Map<String, Object> airportAttributes = new HashMap<>();
        airportAttributes.put("isStart",isStart);
        List<Option> nearbyAirportOptionList = resolveNearbyAirportOptionList(location,airline);
        airportAttributes.put("nearbyAirportOptionList",nearbyAirportOptionList);
        AirportCodes airportCodes = new AirportCodes();
        if (location!= null) airportCodes.setCodes(location.getAirports());
        airportAttributes.put("airportCodes",airportCodes);
        return new ModelAndView("fragments/trips :: pin-airports",airportAttributes);
    }


    private List<Option> resolveNearbyAirportOptionList(Location location, Airline airline) {
        if (location == null) return null;
        Double longitude = location.getLongitude();
        Double latitude = location.getLatitude();
        List<Airport> nearbyAirportList;
        if (airline != null) nearbyAirportList = voyagerService.nearbyAirports(latitude,longitude,5,airline);
        else nearbyAirportList = voyagerService.nearbyAirportsAllActiveAirlines(latitude,longitude,5);
        return nearbyAirportList.stream().map(nearbyAirport -> {
            Option option = buildAirportOptionForInput(nearbyAirport);
            option.setDisabled(location.getAirports().contains(nearbyAirport.getIata()));
            return option;
        }).toList();
    }

    private ModelAndView replaceNearbyAirports(Boolean isStart, Airport airport, Location location) {
        Map<String, Object> airportAttributes = new HashMap<>();
        airportAttributes.put("isStart",isStart);
        List<Option> nearbyAirportOptionList = null;
        if (airport != null) {
            Double longitude = airport.getLongitude();
            Double latitude = airport.getLatitude();
            List<Airport> nearbyAirportList = voyagerService.nearbyAirports(latitude,longitude,5,Airline.DELTA);
            nearbyAirportOptionList = nearbyAirportList.stream().map(this::buildAirportOptionForSelect).toList();
        } else if (location != null) {
            Double longitude = location.getLongitude();
            Double latitude = location.getLatitude();
            List<Airport> nearbyAirportList = voyagerService.nearbyAirports(latitude,longitude,5,Airline.DELTA);
            nearbyAirportOptionList = nearbyAirportList.stream().map(this::buildAirportOptionForSelect).toList();
        }
        airportAttributes.put("nearbyAirportOptionList",nearbyAirportOptionList);
        return new ModelAndView("fragments/trips :: replace-nearby-airports",airportAttributes);
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
                LOGGER.error(String.format("getAirports called with airportFilter '%s', not yet implemented", airportFilter.name()));
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("airportFilter '%s' not yet implemented for fetching with location", airportFilter));
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
                LOGGER.error(String.format("getAirports called with airportFilter '%s', not yet implemented", airportFilter.name()));
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("airportFilter '%s' not yet implemented for fetching with location", airportFilter));
            }
        }
    }

    private void populateDeltaRoute(Map<String, Object> mavAttributes,
                                    String startAirport, String endAirport,
                                    String closerStartAirport, String closerEndAirport,
                                    PathExclusions pathExclusions) {
        String deltaStart = null;
        String deltaEnd = null;
        if (voyagerService.isDeltaIataCode(startAirport)) {
            deltaStart = startAirport;
            mavAttributes.put("startAirport", voyagerService.getAirport(startAirport));
        }
        if (voyagerService.isDeltaIataCode(endAirport)) {
            deltaEnd = endAirport;
            mavAttributes.put("endAirport", voyagerService.getAirport(endAirport));
        }

        if (deltaStart != null && deltaEnd != null) {
            if (voyagerService.isDeltaIataCode(deltaStart) && voyagerService.isValidIataCode(closerStartAirport)) {
                PathResponse<PathAirline> pathResponse = voyagerService.getPath(List.of(closerStartAirport),List.of(deltaStart));
                List<PathAirline> pathAirlineList = pathResponse.getResponseList();
                List<Route> closerStartRouteList = pathAirlineList.get(0).getRouteList();
                List<Airport[]> closerStartRouteAirportList = new ArrayList<>();
                List<List<Flight>> closerStartRouteFlightList = new ArrayList<>();
                for (Route route : closerStartRouteList)
                    addAirportFlightsFromRoute(route,closerStartRouteAirportList,closerStartRouteFlightList);
                mavAttributes.put("closerStartRouteList",closerStartRouteList);
                mavAttributes.put("closerStartRouteAirportList",closerStartRouteAirportList);
                mavAttributes.put("closerStartRouteFlightList",closerStartRouteFlightList);
            }

            if (voyagerService.isDeltaIataCode(deltaEnd) && voyagerService.isValidIataCode(closerEndAirport)) {
                PathResponse<PathAirline> pathResponse = voyagerService.getPath(List.of(deltaEnd),List.of(closerEndAirport));
                List<PathAirline> pathAirlineList = pathResponse.getResponseList();
                List<Route> closerEndRouteList = pathAirlineList.get(0).getRouteList();
                List<Airport[]> closerEndRouteAirportList = new ArrayList<>();
                List<List<Flight>> closerEndRouteFlightList = new ArrayList<>();
                for (Route route : closerEndRouteList)
                    addAirportFlightsFromRoute(route,closerEndRouteAirportList,closerEndRouteFlightList);
                mavAttributes.put("closerEndRouteList",closerEndRouteList);
                mavAttributes.put("closerEndRouteAirportList",closerEndRouteAirportList);
                mavAttributes.put("closerEndRouteFlightList",closerEndRouteFlightList);
            }

            List<Route> routeList = new ArrayList<>();
            List<Airport[]> routeAirportList = new ArrayList<>();
            List<List<Flight>> routeFlightList = new ArrayList<>();

            List<Route> excludedRouteList = new ArrayList<>();
            List<Airport[]> excludedRouteAirportList = new ArrayList<>();
            List<List<Flight>> excludedRouteFlightList = new ArrayList<>();
            fillLists(deltaStart,deltaEnd,routeList,routeAirportList,routeFlightList,
                    excludedRouteList,excludedRouteAirportList,excludedRouteFlightList,pathExclusions);
            if (excludedRouteList.isEmpty()) excludedRouteList = null;
            mavAttributes.put("routeAirportList", routeAirportList);
            mavAttributes.put("routeList", routeList);
            mavAttributes.put("routeFlightList", routeFlightList);
            mavAttributes.put("excludedRouteList", excludedRouteList);
            mavAttributes.put("excludedRouteAirportList", excludedRouteAirportList);
            mavAttributes.put("excludedRouteFlightList", excludedRouteFlightList);
            mavAttributes.put("pathExclusions", pathExclusions);
        }
    }

    private void addAirportFlightsFromRoute(Route route, List<Airport[]> routeAirportList, List<List<Flight>> routeFlightList) {
        Airport origin = voyagerService.getAirport(route.getOrigin());
        Airport destination = voyagerService.getAirport(route.getDestination());
        routeAirportList.add(new Airport[]{origin, destination});
        List<Flight> flightList = voyagerService.getFlights(route.getId(),true);
        flightList.forEach(flight -> {
            if (flight.getZonedDateTimeArrival().toInstant().isBefore(flight.getZonedDateTimeDeparture().toInstant())) {
                flight.setZonedDateTimeArrival(flight.getZonedDateTimeArrival().plusDays(1));
            }
            flight.setDuration(Duration.between(
                    flight.getZonedDateTimeDeparture().toInstant(), flight.getZonedDateTimeArrival().toInstant()));
            flight.setZonedDateTimeDeparture(
                    flight.getZonedDateTimeDeparture().withZoneSameInstant(origin.getZoneId()));
            flight.setZonedDateTimeArrival(
                    flight.getZonedDateTimeArrival().withZoneSameInstant(destination.getZoneId()));
        });
        flightList.sort(Comparator.comparing((flight -> flight.getZonedDateTimeDeparture().toLocalTime())));
        routeFlightList.add(flightList);
    }

    private void fillLists(String deltaStart, String deltaEnd, List<Route> routeList, List<Airport[]> routeAirportList,
                           List<List<Flight>> routeFlightList,
                           List<Route> excludedRouteList, List<Airport[]> excludedRouteAirportList,
                           List<List<Flight>> excludedRouteFlightList, PathExclusions pathExclusions) {
        if (!voyagerService.isValidIataCode(deltaStart) || !voyagerService.isValidIataCode(deltaEnd)) return;
        List<PathAirline> pathAirlineList = null;
        List<String> excludeAirports = List.of();
        List<Integer> excludeRouteIds = List.of();
        if (pathExclusions != null && pathExclusions.getHasExclusions()) {
            List<Integer> excludedRouteIds = pathExclusions.getRouteIds();
            for (Integer routeId : excludedRouteIds) {
                Route route = voyagerService.getRoute(routeId);
                Airport origin = voyagerService.getAirport(route.getOrigin());
                Airport destination = voyagerService.getAirport(route.getDestination());
                excludedRouteAirportList.add(new Airport[]{origin, destination});
                excludedRouteList.add(route);
                List<Flight> flightList = voyagerService.getFlights(route.getId(),true,Airline.DELTA);
                flightList.forEach(flight -> {
                    if (flight.getZonedDateTimeArrival().toInstant().isBefore(flight.getZonedDateTimeDeparture().toInstant())) {
                        flight.setZonedDateTimeArrival(flight.getZonedDateTimeArrival().plusDays(1));
                    }
                    flight.setDuration(Duration.between(
                            flight.getZonedDateTimeDeparture().toInstant(), flight.getZonedDateTimeArrival().toInstant()));
                    flight.setZonedDateTimeDeparture(
                            flight.getZonedDateTimeDeparture().withZoneSameInstant(origin.getZoneId()));
                    flight.setZonedDateTimeArrival(
                            flight.getZonedDateTimeArrival().withZoneSameInstant(destination.getZoneId()));
                });
                flightList.sort(Comparator.comparing((flight -> flight.getZonedDateTimeDeparture().toLocalTime())));
                excludedRouteFlightList.add(flightList);
            }
        }
        pathAirlineList = voyagerService.getPath(List.of(deltaStart), List.of(deltaEnd), excludeAirports, excludeRouteIds,Airline.DELTA).getResponseList();

        for (Route route : pathAirlineList.get(0).getRouteList()) {
            Airport origin = voyagerService.getAirport(route.getOrigin());
            Airport destination = voyagerService.getAirport(route.getDestination());
            routeAirportList.add(new Airport[]{origin, destination});
            routeList.add(route);
            List<Flight> flightList = voyagerService.getFlights(route.getId(),true,Airline.DELTA);
            flightList.forEach(flight -> {
                if (flight.getZonedDateTimeArrival().toInstant().isBefore(flight.getZonedDateTimeDeparture().toInstant())) {
                    flight.setZonedDateTimeArrival(flight.getZonedDateTimeArrival().plusDays(1));
                }
                flight.setDuration(Duration.between(
                        flight.getZonedDateTimeDeparture().toInstant(), flight.getZonedDateTimeArrival().toInstant()));
                flight.setZonedDateTimeDeparture(
                        flight.getZonedDateTimeDeparture().withZoneSameInstant(origin.getZoneId()));
                flight.setZonedDateTimeArrival(
                        flight.getZonedDateTimeArrival().withZoneSameInstant(destination.getZoneId()));
            });
            flightList.sort(Comparator.comparing((flight -> flight.getZonedDateTimeDeparture().toLocalTime())));
            routeFlightList.add(flightList);
        }
    }
}
