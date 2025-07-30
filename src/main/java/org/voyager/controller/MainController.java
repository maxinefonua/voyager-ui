package org.voyager.controller;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import org.voyager.model.*;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.location.Location;
import org.voyager.model.response.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;
import org.voyager.model.route.Path;
import org.voyager.model.route.PathAirline;
import org.voyager.model.route.Route;
import org.voyager.service.VoyagerService;

import java.util.*;
import java.util.stream.Collectors;

import static org.voyager.utils.ConstantsUI.*;

@Controller
public class MainController {
    private static final DefaultPage DEFAULT_PAGE = DefaultPage.SAVED;

    @Autowired
    private VoyagerService voyagerService;

    @Autowired
    private TripsController tripsController;

    @Autowired
    private SavedController savedController;

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    @GetMapping("/hello-world")
    public String helloWorld(Model model) {
        model.addAttribute("message","Hello World");
        return "hello-world";
    }

    @GetMapping("/")
    public String homepage(Model model) {
        List<Location> locations = voyagerService.getLocations();
        model.addAttribute("locations",locations);
        model.addAttribute("defaultPage",DEFAULT_PAGE);
        model.addAttribute("lookupAttribution", voyagerService.lookupAttribution());
        switch (DEFAULT_PAGE) {
            case TRIPS -> {
                tripsController.addDefaultAttributes(model);
            }
            case SAVED -> {
                savedController.addDefaultAttributes(model);
            }
        }
        return "index";
    }

    @GetMapping("/general")
    public String generalPage() {
        return "general";
    }

    @GetMapping("/add")
    public String getAdd(Model model) {
        model.addAttribute("lookupAttribution", voyagerService.lookupAttribution());
        return "fragments/tab :: add-tab";
    }

    @GetMapping("/airports")
    @Cacheable("airportsCache")
    public String getAirports(Model model, @RequestParam(AIRPORT_FILTER_PARAM_NAME) AirportFilter airportFilter) {
        List<Airport> airportList = new ArrayList<>();
        switch (airportFilter) {
            case DELTA -> airportList.addAll(voyagerService.airports(Airline.DELTA));
            case CIVIL -> airportList.addAll(voyagerService.airports(AirportType.CIVIL));
            case MILITARY -> airportList.addAll(voyagerService.airports(AirportType.MILITARY));
            case ALL -> airportList.addAll(voyagerService.airports(Arrays.asList(AirportType.CIVIL,AirportType.MILITARY)));
        }
        model.addAttribute("airportList",airportList);
        return "fragments/options :: iata-code-list";
    }

    @GetMapping("/locations")
    public String getLocations(Model model) {
        List<Location> locationList = voyagerService.getLocations();
        model.addAttribute("locationList",locationList);
        return "fragments/options :: saved-location-list";
    }

    @GetMapping("/reverse-trip")
    @Cacheable("reverseCache")
    public Collection<ModelAndView> reverseTrip(@RequestParam(required = false) String startAirportCode,
                                                @RequestParam(required = false) String endAirportCode,
                                                @RequestParam(required = false) Integer startLocationId,
                                                @RequestParam(required = false) Integer endLocationId,
                                                @RequestParam(required = false) String nonDeltaStartCode,
                                                @RequestParam(required = false) String nonDeltaEndCode) {
        return resetAirportReview(endAirportCode,startAirportCode,endLocationId,startLocationId,nonDeltaEndCode,nonDeltaStartCode);
    }

    @GetMapping("/closer-airports")
    @Cacheable("closerAirportsCache")
    public String closerAirports(Model model, @RequestParam Double latitude, @RequestParam Double longitude, @RequestParam Integer iterIndex) {
        LOGGER.debug("closerAirports called with latitude: " + latitude + ", longitude: " + longitude + ", iterIndex: " + iterIndex);
        List<Airport> nearbyDeltaAirports = voyagerService.nearbyAirports(latitude,longitude,5,Airline.DELTA);
        List<Airport> civilList = new ArrayList<>(voyagerService.nearbyAirports(latitude,longitude,10,AirportType.CIVIL));
        Set<String> airlineCodes = nearbyDeltaAirports.stream().map(Airport::getIata).collect(Collectors.toSet());
        List<Airport> filtered = civilList.stream().filter(airport -> !airlineCodes.contains(airport.getIata())).toList();
        List<Airport> closer = new ArrayList<>();
        for (Airport airport : filtered) {
            if (airport.getDistance() >= nearbyDeltaAirports.get(0).getDistance()) break;
            closer.add(airport);
        }
        closer = closer.stream().limit(5).toList();
        if (!closer.isEmpty())  {
            model.addAttribute("nonDeltaAirportList",closer);
            model.addAttribute("closestAirportIata",closer.get(0).getIata());
            model.addAttribute("iterIndex",iterIndex);
        }
        return "fragments/form :: non-delta-airport-not-trips";
    }

    @GetMapping("/search")
    public Collection<ModelAndView> search(Model model, @ModelAttribute(SEARCH_TEXT_ATTRIBUTE_NAME) String searchText)  {
        long beforeSearch = System.currentTimeMillis();
        SearchResult<ResultDetails> voyagerResponse = voyagerService.lookupWithDetails(searchText,0,5);
        List<ResultDetails> lookupResults = voyagerResponse.getResults();
        Integer totalResultsCount = voyagerResponse.getResultCount();
        double duration = (System.currentTimeMillis() - beforeSearch)/1000.0;
        LOGGER.debug("duration of search: " + duration + "s");
        LOGGER.debug("retrieved [" + lookupResults.size() + "] of [" + totalResultsCount + "] lookup results");
        return List.of(
                new ModelAndView("fragments/search :: search-location-results",
                        Map.of("lookupResults", lookupResults,
                                "totalResultsCount",totalResultsCount,
                                "searchText",searchText)),
                new ModelAndView("fragments/search :: lookupFooterResults",
                        Map.of("totalResultsCount",totalResultsCount)));
    }

    public ModelAndView getUpdatedReviewPath(Location startLocation, Location endLocation, Airport startAirport, Airport endAirport, Airport nonDeltaStartAirport, Airport nonDeltaEndAirport) {
        Map<String,Object> attributes = new HashMap<>();
        if (startLocation != null) attributes.put("startLocation", startLocation);
        if (endLocation != null) attributes.put("endLocation", endLocation);
        if (startAirport != null) attributes.put("startAirport", startAirport);
        if (endAirport != null) attributes.put("endAirport", endAirport);
        if (nonDeltaStartAirport != null) attributes.put("nonDeltaStartAirport",nonDeltaStartAirport);
        if (nonDeltaEndAirport != null) attributes.put("nonDeltaEndAirport",nonDeltaEndAirport);
        return new ModelAndView("fragments/trips :: review-path", attributes);
    }

    @GetMapping("/review-airport-reset")
    public Collection<ModelAndView> resetAirportReview(@RequestParam(required = false) String startAirportCode,
                                                       @RequestParam(required = false) String endAirportCode,
                                                       @RequestParam(required = false) Integer startLocationId,
                                                       @RequestParam(required = false) Integer endLocationId,
                                                       @RequestParam(required = false) String nonDeltaStartCode,
                                                       @RequestParam(required = false) String nonDeltaEndCode) {
        Location startLocation = null, endLocation = null;
        if (startLocationId != 0) startLocation = voyagerService.getLocation(startLocationId);
        if (endLocationId != 0) endLocation = voyagerService.getLocation(endLocationId);

        Airport startAirport = null, endAirport = null;
        if (voyagerService.isDeltaIataCode(startAirportCode)) startAirport = voyagerService.getAirport(startAirportCode);
        else if (voyagerService.isValidIataCode(startAirportCode) && StringUtils.isBlank(nonDeltaStartCode)) nonDeltaStartCode = startAirportCode;
        if (voyagerService.isDeltaIataCode(endAirportCode)) endAirport = voyagerService.getAirport(endAirportCode);
        else if (voyagerService.isValidIataCode(endAirportCode) && StringUtils.isBlank(nonDeltaEndCode)) nonDeltaEndCode = endAirportCode;

        Airport nonDeltaStartAirport = null, nonDeltaEndAirport = null;
        if (voyagerService.isValidIataCode(nonDeltaStartCode)) nonDeltaStartAirport = voyagerService.getAirport(nonDeltaStartCode);
        if (voyagerService.isValidIataCode(nonDeltaEndCode)) nonDeltaEndAirport = voyagerService.getAirport(nonDeltaEndCode);

        return List.of(getUpdatedReviewPath(startLocation,endLocation,startAirport,endAirport, nonDeltaStartAirport, nonDeltaEndAirport));
    }

    @GetMapping("/test")
    public String testPage() {
        return "test";
    }
}
