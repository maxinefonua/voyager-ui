package org.voyager.controller;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.voyager.utils.ConstantsUtils.ALPHA2_CODE_REGEX;
import static org.voyager.utils.ConstantsUtils.SOURCE_ID_PARAM_NAME;

@Controller
public class TripsController {
    private static final TripFilter DEFAULT_TRIP_FILTER = TripFilter.AIRPORT;
    private static final AirportFilter DEFAULT_AIRPORT_FILTER = AirportFilter.CIVIL;
    private static final AirportFilter DEFAULT_CLOSER_FILTER = AirportFilter.CIVIL;
    private static final Logger LOGGER = LoggerFactory.getLogger(TripsController.class);
    private static LocationServiceAPI locationServiceAPI;
    private static FlightServiceAPI flightServiceAPI;
    private static CountryServiceAPI countryServiceAPI;
    private static SearchServiceAPI searchServiceAPI;
    private static AirportServiceAPI airportServiceAPI;
    private static PathServiceAPI pathServiceAPI;
    private Source source;

    // TODO: handle delete location removal correctly - duplicates showing up in recent locations
    private Deque<Location> recentLocations = new ArrayDeque<>();

    @Autowired
    private VoyagerService voyagerService;

    @PostConstruct
    public void init() {
        flightServiceAPI = voyagerService.getFlightServiceAPI();
        locationServiceAPI = voyagerService.getLocationServiceAPI();
        countryServiceAPI = voyagerService.getCountryServiceAPI();
        searchServiceAPI = voyagerService.getSearchServiceAPI();
        airportServiceAPI = voyagerService.getAirportServiceAPI();
        pathServiceAPI = voyagerService.getPathServiceAPI();
    }

    void removeDeletedLocationFromRecents(Integer deletedLocationId) {
        Optional<Location> optionalLocation = recentLocations.stream()
                .filter(location -> location.getId().equals(deletedLocationId)).findAny();
        optionalLocation.ifPresent(location -> recentLocations.remove(location));
    }

    void addDefaultAttributes(Model model) {
        model.addAttribute("lookupAttribution", searchServiceAPI.getLookupAttribution());
        if (recentLocations.isEmpty())
            recentLocations.addAll(locationServiceAPI.getLocations(10));
        List<Option> optionList = recentLocations.stream()
                .map(this::buildLocationOptionForInput)
                .toList();
        model.addAttribute("startOptionList", optionList);
        model.addAttribute("endOptionList", optionList);
    }

    @GetMapping("/build-path")
    public String buildPath(Model model, @RequestParam(name = SOURCE_ID_PARAM_NAME) String[] sourceIds,
                            PathExclusions pathExclusions) {
        Location startLocation = null;
        Location endLocation = null;
        List<String> startAirportCodes = List.of();
        List<String> endAirportCodes = List.of();
        if (StringUtils.isNotBlank(sourceIds[0])) startLocation = locationServiceAPI.getLocation(source,sourceIds[0]);
        if (StringUtils.isNotBlank(sourceIds[1])) endLocation = locationServiceAPI.getLocation(source,sourceIds[1]);
        if (startLocation != null && endLocation != null) {
            startAirportCodes = startLocation.getAirports();
            endAirportCodes = endLocation.getAirports();
            if (!startAirportCodes.isEmpty() && !endAirportCodes.isEmpty()) {
                List<Airline> airlineListStart = airportServiceAPI.getAirlines(startAirportCodes);
                List<Airline> airlineListEnd = airportServiceAPI.getAirlines(endAirportCodes);
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
                    pathAirports.add(toAdd);
                    for (Route route : pathAirline.getRouteList()) {
                        toAdd = airportServiceAPI.getAirport(route.getDestination());
                        if (toAdd.getCountryCode().matches(ALPHA2_CODE_REGEX))
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
        // TODO: when location deleted, how to update this recent locations?
        if (recentLocations.isEmpty()) recentLocations.addAll(locationServiceAPI.getLocations(10));
        Location startLocation = startLocationId == null ? null : locationServiceAPI.getLocation(startLocationId);
        Location endLocation = endLocationId == null ? null : locationServiceAPI.getLocation(endLocationId);
        if (startLocation != null) {
            if (recentLocations.contains(startLocation)) recentLocations.remove(startLocation);
            else recentLocations.removeLast();
            recentLocations.push(startLocation);
            Option startOption = buildLocationOptionForInput(startLocation);
            model.addAttribute("startInputText", startOption.getDisplay());
        }
        if (endLocation != null) {
            if (recentLocations.contains(endLocation)) recentLocations.remove(endLocation);
            else recentLocations.removeLast();
            recentLocations.push(endLocation);
            Option endOption = buildLocationOptionForInput(endLocation);
            model.addAttribute("endInputText", endOption.getDisplay());
        }
        List<Option> optionList = recentLocations.stream()
                .map(this::buildLocationOptionForInput).toList();
        model.addAttribute("startOptionList", optionList);
        model.addAttribute("endOptionList", optionList);
        model.addAttribute("mapHidden",mapHidden);
        return "fragments/tab :: trips-tab";
    }

    @GetMapping("/lookup")
    public String lookup(Model model, String inputText, String excludeSourceId)  {
        SearchResult<ResultSearch> searchResult = searchServiceAPI.search(inputText,10);
        List<ResultSearch> lookupResults = searchResult.getResults();
        List<Option> optionList = new ArrayList<>(lookupResults.stream().map(resultSearch -> {
            String displayText = String.format("%s, %s | %s", resultSearch.getName(),
                    resultSearch.getSubdivision(), resultSearch.getCountryName());
            String valueText = resultSearch.getSourceId();
            Boolean disabled = !StringUtils.isBlank(excludeSourceId) && excludeSourceId.equals(resultSearch.getSourceId());
            return Option.builder().display(displayText).value(valueText).disabled(disabled).build();
        }).toList());
        model.addAttribute("optionList",optionList);
        model.addAttribute("tripFilter",TripFilter.LOCATION);
        return "fragments/options :: trip-input-options";
    }

    @GetMapping("/update-review")
    public String updateReview(Model model, Boolean isStart, String sourceId) {
        Location location = null;
        if (StringUtils.isNotBlank(sourceId)) {
            try {
                if (source == null) source = searchServiceAPI.getSource();
                location = locationServiceAPI.getLocation(source, sourceId);
            } catch (ResponseStatusException e) {
                ResultSearchFull resultSearchFull = searchServiceAPI.fetchResultSearchFull(sourceId);
                if (resultSearchFull.getBbox() == null)
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            String.format("Failed to create location form for sourceId: %s",sourceId));
                LocationForm locationForm = LocationMapperUtils.toLocationForm(resultSearchFull);
                location = locationServiceAPI.addLocation(locationForm);
            }
            if (!recentLocations.contains(location))
                recentLocations.push(location);
        }
        model.addAttribute("isStart",isStart);
        List<Option> nearbyAirportOptionList = resolveNearbyAirportOptionList(location);
        model.addAttribute("nearbyAirportOptionList",nearbyAirportOptionList);
        AirportCodes newAirportCodes = new AirportCodes();
        if (location!= null) newAirportCodes.setCodes(location.getAirports());
        model.addAttribute("airportCodes",newAirportCodes);
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
        if (airportServiceAPI.isValidIataCode(airportCode) && !airportCodes.getCodes().contains(airportCode))
            airportCodes.getCodes().add(airportCode);
        Location location = locationServiceAPI.getLocation(source,sourceId);
        LocationPatch locationPatch = LocationPatch.builder().airports(airportCodes.getCodes()).build();
        locationServiceAPI.patchLocation(location.getId(),locationPatch);
        model.addAttribute("airportCodes",airportCodes);
        model.addAttribute("isStart",isStart);
        return "fragments/trips :: airport-codes";
    }

    @GetMapping("/remove-airport-code")
    public String removeAirportCode(Model model, Boolean isStart, @ModelAttribute AirportCodes airportCodes,
                                    String airportCode, String sourceId) {
        airportCodes.getCodes().remove(airportCode);
        Location location = locationServiceAPI.getLocation(source,sourceId);
        LocationPatch locationPatch = LocationPatch.builder().airports(airportCodes.getCodes()).build();
        locationServiceAPI.patchLocation(location.getId(),locationPatch);
        model.addAttribute("airportCodes",airportCodes);
        model.addAttribute("isStart",isStart);
        return "fragments/trips :: airport-codes";
    }

    @GetMapping("/flights")
    public String getFlights(Model model,Integer routeId,
                             String startZoneId, String endZoneId,
                             Airline airline, Integer pathIterIndex) {
        List<Flight> flightList = flightServiceAPI.getFlights(routeId,airline);
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
        Country country = countryServiceAPI.getCountry(airport.getCountryCode());
        return Option.builder()
                .elementName(airport.getIata())
                .name(airport.getName())
                .city(airport.getCity())
                .subdivision(airport.getSubdivision())
                .country(country.getName())
                .longitude(airport.getLongitude())
                .latitude(airport.getLatitude())
                .display(String.format("%s | %s, %s of %s", airport.getName(), airport.getCity(),
                        airport.getSubdivision(),country.getName()))
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

    private List<Option> resolveNearbyAirportOptionList(Location location) {
        if (location == null) return null;
        Double longitude = location.getLongitude();
        Double latitude = location.getLatitude();
        List<Airport> nearbyAirportList = airportServiceAPI.nearbyAirports(latitude,longitude,5,Arrays.asList(Airline.values()));
        return nearbyAirportList.stream().map(nearbyAirport -> {
            Option option = buildAirportOptionForInput(nearbyAirport);
            option.setDisabled(location.getAirports().contains(nearbyAirport.getIata()));
            return option;
        }).toList();
    }
}
