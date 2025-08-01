package org.voyager.controller;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.model.*;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.country.Country;
import org.voyager.model.flight.Flight;
import org.voyager.model.location.*;
import org.voyager.model.response.SearchResult;
import org.voyager.model.result.ResultSearch;
import org.voyager.model.result.ResultSearchFull;
import org.voyager.model.route.PathAirline;
import org.voyager.model.route.PathResponse;
import org.voyager.model.route.Route;
import org.voyager.service.*;
import org.voyager.service.impl.*;
import org.voyager.utils.LocationMapperUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.voyager.utils.ConstantsUtils.SOURCE_ID_PARAM_NAME;

@Controller
public class TripsController {
    private static final TripFilter DEFAULT_TRIP_FILTER = TripFilter.AIRPORT;
    private static final AirportFilter DEFAULT_AIRPORT_FILTER = AirportFilter.CIVIL;
    private static final AirportFilter DEFAULT_CLOSER_FILTER = AirportFilter.CIVIL;
    private static final Logger LOGGER = LoggerFactory.getLogger(TripsController.class);
    private static LocationServiceAPI locationServiceAPI;
    private static CountryServiceAPI countryServiceAPI;
    private static SearchServiceAPI searchServiceAPI;
    private static AirportServiceAPI airportServiceAPI;
    private static PathServiceAPI pathServiceAPI;

    private Stack<Location> recentLocations = new Stack<>();


    @Autowired
    private VoyagerService voyagerService;

    @PostConstruct
    public void init() {
        locationServiceAPI = voyagerService.getLocationServiceAPI();
        countryServiceAPI = voyagerService.getCountryServiceAPI();
        searchServiceAPI = voyagerService.getSearchServiceAPI();
        airportServiceAPI = voyagerService.getAirportServiceAPI();
        pathServiceAPI = voyagerService.getPathServiceAPI();
    }

    void addDefaultAttributes(Model model) {
        recentLocations.clear();
        Long before = System.currentTimeMillis();
        recentLocations.addAll(locationServiceAPI.getLocations(10));
        Long duration = System.currentTimeMillis() - before;
        LOGGER.info(String.format("locationServiceAPI getLocations returned after %d ms",duration));
        List<Option> optionList = recentLocations.stream()
                .map(this::buildLocationOptionForInput)
                .toList();
        model.addAttribute("startOptionList", optionList);
        model.addAttribute("endOptionList", optionList);
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
        Location startLocation = locationServiceAPI.getLocation(startLocationId);
        Location endLocation = locationServiceAPI.getLocation(endLocationId);
        List<String> originList = startLocation.getAirports();
        List<String> destinationList = endLocation.getAirports();
        if (!originList.isEmpty() && !destinationList.isEmpty()) {
            PathResponse<PathAirline> pathResponse;
            if (airlineSelection.equals("ALL"))
                pathResponse = pathServiceAPI.getPathAirlineList(originList, destinationList,
                        pathExclusions.getAirports(), pathExclusions.getRouteIds());
            else pathResponse = pathServiceAPI.getPathAirlineList(originList, destinationList,
                    pathExclusions.getAirports(), pathExclusions.getRouteIds(), Airline.valueOf(airlineSelection));
            List<PathAirline> pathAirlineList = pathResponse.getResponseList();
            List<List<Airport>> pathAirportsList = new ArrayList<>();
            if (!pathAirlineList.isEmpty()) {
                for (PathAirline pathAirline : pathAirlineList) {
                    List<Airport> pathAirports = new ArrayList<>();
                    Airport toAdd = airportServiceAPI.getAirport(pathAirline.getRouteList().get(0).getOrigin());
                    toAdd.setCountryCode(countryServiceAPI.getCountry(toAdd.getCountryCode()).getName());
                    pathAirports.add(toAdd);
                    for (Route route : pathAirline.getRouteList()) {
                        toAdd = airportServiceAPI.getAirport(route.getDestination());
                        toAdd.setCountryCode(countryServiceAPI.getCountry(toAdd.getCountryCode()).getName());
                        pathAirports.add(toAdd);
                    }
                    pathAirportsList.add(pathAirports);
                }
            }
            model.addAttribute("pathAirlineList",pathAirlineList);
            model.addAttribute("pathAirportsList",pathAirportsList);
        }
        return "fragments/trips :: path-airline";
    }

    @GetMapping("/trips")
    public String getTrips(Model model, Integer endLocationId, Integer startLocationId, Boolean mapHidden) {
        if (startLocationId == null && endLocationId == null) addDefaultAttributes(model);
        else {
            if (startLocationId != null) {
                Location startLocation = locationServiceAPI.getLocation(startLocationId);
                Option locationOption = buildLocationOptionForInput(startLocation);
                model.addAttribute("startInputText", locationOption.getDisplay());
                model.addAttribute("startOptionList", List.of(locationOption));
            }
            if (endLocationId != null) {
                Location endLocation = locationServiceAPI.getLocation(endLocationId);
                Option locationOption = buildLocationOptionForInput(endLocation);
                model.addAttribute("endInputText", locationOption.getDisplay());
                model.addAttribute("endOptionList", List.of(locationOption));
            }
        }
        model.addAttribute("mapHidden",mapHidden);
        return "fragments/tab :: trips-tab";
    }

    @GetMapping("/lookup")
    public String lookup(Model model, String inputText)  {
        SearchResult<ResultDetails> voyagerResponse = voyagerService.lookupWithDetails(inputText, 0, 10);
        List<ResultDetails> lookupResults = voyagerResponse.getResults();
//        Integer totalResultsCount = voyagerResponse.getResultCount();
        List<Option> optionList = new ArrayList<>(lookupResults.stream().map(resultDetails -> {
            ResultSearch resultSearch = resultDetails.getResultSearch();
            String displayText = String.format("%s, %s | %s", resultSearch.getName(),
                    resultSearch.getSubdivision(), resultSearch.getCountryName());
            String valueText = resultSearch.getSourceId();
            return Option.builder().display(displayText).value(valueText).build();
        }).toList());
        model.addAttribute("optionList",optionList);
        model.addAttribute("tripFilter",TripFilter.LOCATION);
        return "fragments/options :: trip-input-options";
    }

    @GetMapping("/update-review")
    public String updateReview(Model model, Boolean isStart, String sourceId) {
        Location location = null;
        if (StringUtils.isNotBlank(sourceId)) {
            Source source = Source.valueOf(voyagerService.lookupAttribution().getName().toUpperCase());
            try {
                location = locationServiceAPI.getLocation(source, sourceId);
            } catch (ResponseStatusException e) {
                ResultSearchFull resultSearchFull = searchServiceAPI.fetchResultSearchFull(sourceId);
                if (resultSearchFull.getBbox() == null)
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            String.format("Failed to create location form for sourceId: %s",sourceId));
                LocationForm locationForm = LocationMapperUtils.toLocationForm(resultSearchFull);
                location = locationServiceAPI.addLocation(locationForm);
            }
            recentLocations.remove(location);
            recentLocations.push(location);
        }
        model.addAttribute("isStart",isStart);
        List<Option> nearbyAirportOptionList = resolveNearbyAirportOptionList(location,null);
        model.addAttribute("nearbyAirportOptionList",nearbyAirportOptionList);
        AirportCodes airportCodes = new AirportCodes();
        if (location!= null) airportCodes.setCodes(location.getAirports());
        model.addAttribute("airportCodes",airportCodes);
        model.addAttribute("location",location);
        return "fragments/trips :: pin-airports";
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

    @GetMapping("/flights")
    public String getFlights(Model model,Integer routeId,
                             String startZoneId, String endZoneId,
                             Airline airline, Integer pathIterIndex) {
        List<Flight> flightList = voyagerService.getFlights(routeId,true,airline);
        flightList.forEach(flight -> {
            while (flight.getZonedDateTimeArrival().isBefore(flight.getZonedDateTimeDeparture())) {
                flight.setZonedDateTimeArrival(flight.getZonedDateTimeArrival().plusDays(1));
            }
            flight.setDuration(Duration.between(flight.getZonedDateTimeDeparture(), flight.getZonedDateTimeArrival()));
        });
        flightList.sort(Comparator.comparing(Flight::getZonedDateTimeDeparture));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a");
        DateTimeFormatter departureFormatter = DateTimeFormatter.ofPattern("HH:mm");
        List<FlightDetails> flightDetailsList = flightList.stream().map(flight -> {
            Duration duration = flight.getDuration();
            StringJoiner durationString = new StringJoiner(" ");
            if (duration.toDaysPart() > 0)
                durationString.add(String.format("%d%s",duration.toDaysPart(),"days"));
            if (duration.toHoursPart() > 0)
                durationString.add(String.format("%d%s",duration.toHoursPart(),"hrs"));
            if (duration.toMinutesPart() > 0)
                durationString.add(String.format("%d%s",duration.toMinutesPart(),"mns"));
            return
                FlightDetails.builder().flightNumber(flight.getFlightNumber())
                        .departureTimeFormatted(departureFormatter.format(flight.getZonedDateTimeDeparture()
                                .withZoneSameInstant(ZoneId.of(startZoneId))))
                        .arrivalTimeFormatted(dateTimeFormatter.format(flight.getZonedDateTimeArrival()
                                .withZoneSameInstant(ZoneId.of(endZoneId))))
                        .durationFormatted(durationString.toString())
                        .build();
        }).sorted(Comparator.comparing(FlightDetails::getDepartureTimeFormatted)).toList();
        flightDetailsList.forEach(flightDetails -> {
            LocalTime localTime = LocalTime.parse(flightDetails.getDepartureTimeFormatted(),departureFormatter);
            flightDetails.setDepartureTimeFormatted(localTime.format(dateTimeFormatter));
        });
        model.addAttribute("flightDetailsList",flightDetailsList);
        model.addAttribute("routeId",routeId);
        model.addAttribute("pathIterIndex",pathIterIndex);
        return "fragments/trips :: flights-body";
    }

    private Option buildLocationOptionForInput(Location location) {
        Country country = countryServiceAPI.getCountry(location.getCountryCode());
        String displayText = String.format("%s, %s | %s",location.getName(),location.getSubdivision(),country.getName());
        String valueText = location.getSourceId();
        return Option.builder()
                .display(displayText)
                .value(valueText).build();
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

    private List<Option> getLocationOptionsList(Status status) {
        return voyagerService.getLocations(status).stream().map(location -> Option.builder()
                        .elementName(String.format("%s-%s", location.getName(), location.getId()))
                        .display(String.format("%s, %s in %s", location.getName(),
                                location.getSubdivision(), location.getCountryCode()))
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
                .map(this::buildAirportOption)
                .toList();
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
