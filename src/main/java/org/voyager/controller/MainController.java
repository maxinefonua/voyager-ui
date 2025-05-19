package org.voyager.controller;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.voyager.model.*;
import org.voyager.model.location.LocationDisplay;
import org.voyager.model.location.LocationForm;
import org.voyager.model.response.VoyagerListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;
import org.voyager.model.result.ResultSearch;
import org.voyager.model.route.PathDisplay;
import org.voyager.model.route.RouteDisplay;
import org.voyager.service.VoyagerAPI;
import org.voyager.validate.ValidationUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.voyager.utils.ConstantsUI.*;

@Controller
public class MainController {

    @Autowired
    private VoyagerAPI voyagerAPI;

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    @GetMapping("/hello-world")
    public String helloWorld(Model model) {
        model.addAttribute("message","Hello World");
        return "hello-world";
    }

    @GetMapping("/")
    public String homepage(Model model) {
        List<LocationDisplay> locations = voyagerAPI.getLocations();
        model.addAttribute("locations",locations);
        model.addAttribute("lookupAttribution", voyagerAPI.lookupAttribution());
        return "index";
    }

    @GetMapping("/general")
    public String generalPage() {
        return "general";
    }

    @GetMapping("/saved")
    public String getSaved(Model model) {
        List<LocationDisplay> locations = voyagerAPI.getLocations();
        model.addAttribute("locations",locations);
        return "fragments/tab :: saved-tab";
    }

    @GetMapping("/add")
    public String getAdd(Model model) {
        model.addAttribute("lookupAttribution",voyagerAPI.lookupAttribution());
        return "fragments/tab :: add-tab";
    }

    @PostMapping("/locations")
    public String addLocation(Model model, @ModelAttribute @Valid LocationForm locationForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error -> {
                if (error instanceof FieldError fieldError)LOGGER.error(String.format("'%s' %s",fieldError.getField(),fieldError.getDefaultMessage()));
                else LOGGER.error(error.getDefaultMessage());
            });
            model.addAttribute("locationForm",locationForm);
            return "fragments/form :: add-form-error";
        }
        LocationDisplay saved = voyagerAPI.addLocation(locationForm);
        LOGGER.info("saved: " + saved);
        model.addAttribute("locationForm", locationForm);
        return "fragments/form :: add-form-success";
    }

    @GetMapping("/airports")
    @Cacheable("airportsCache")
    public String getAirports(Model model, @RequestParam(AIRPORT_FILTER_PARAM_NAME) Optional<String> filterOptional) {
        AirportFilter airportFilter = ValidationUtils.resolveAirportFilterOptional(filterOptional);
        Optional<AirportType> type = Optional.empty();
        Optional<Airline> airline = Optional.empty();
        switch (airportFilter) {
            case DELTA -> airline = Optional.of(Airline.DELTA);
            case CIVIL -> type = Optional.of(AirportType.CIVIL);
            case MILITARY -> type = Optional.of(AirportType.MILITARY);
        }
        List<AirportDisplay> airportList = voyagerAPI.airports(type,airline);
        model.addAttribute("airportList",airportList);
        return "fragments/options :: iata-code-list";
    }

    @GetMapping("/trips")
    public String getTrips(Model model) {
        List<LocationDisplay> locations = voyagerAPI.getLocations();
        model.addAttribute("locations",locations);
        model.addAttribute("lookupAttribution", voyagerAPI.lookupAttribution());
        return "fragments/tab :: trips-tab";
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
        List<AirportDisplay> nearbyDeltaAirports = voyagerAPI.nearbyAirports(latitude,longitude,5,Optional.empty(),Optional.of(Airline.DELTA));
        List<AirportDisplay> civilList = new ArrayList<>(voyagerAPI.nearbyAirports(latitude,longitude,10,Optional.of(AirportType.CIVIL),Optional.empty()));
        Set<String> airlineCodes = nearbyDeltaAirports.stream().map(AirportDisplay::getIata).collect(Collectors.toSet());
        List<AirportDisplay> filtered = civilList.stream().filter(airportDisplay -> !airlineCodes.contains(airportDisplay.getIata())).toList();
        List<AirportDisplay> closer = new ArrayList<>();
        for (AirportDisplay airportDisplay : filtered) {
            if (airportDisplay.getDistance() >= nearbyDeltaAirports.get(0).getDistance()) break;
            closer.add(airportDisplay);
        }
        closer = closer.stream().limit(5).toList();
        if (!closer.isEmpty())  {
            model.addAttribute("nonDeltaAirportList",closer);
            model.addAttribute("closestAirportIata",closer.get(0).getIata());
            model.addAttribute("iterIndex",iterIndex);
        }
        return "fragments/form :: non-delta-airport-not-trips";
    }

    @GetMapping("/nearby-airports")
    @Cacheable("nearbyAirportsCache")
    public String nearbyAirports(Model model, @RequestParam Double latitude, @RequestParam Double longitude, @RequestParam(AIRPORT_FILTER_PARAM_NAME) Optional<String> filterOptional) {
        LOGGER.debug("nearbyAirports called with latitude: " + latitude + ", longitude: " + longitude + "airportFilterIsPresent: " + filterOptional.isPresent());
        AirportFilter airportFilter = ValidationUtils.resolveAirportFilterOptional(filterOptional);
        Optional<AirportType> type = Optional.empty();
        Optional<Airline> airline = Optional.empty();
        switch (airportFilter) {
            case DELTA -> airline = Optional.of(Airline.DELTA);
            case CIVIL -> type = Optional.of(AirportType.CIVIL);
            case MILITARY -> type = Optional.of(AirportType.MILITARY);
        }
        List<AirportDisplay> nearbyAirports = voyagerAPI.nearbyAirports(latitude,longitude,5,type,airline);
        model.addAttribute("airportList",nearbyAirports);
        return "fragments/options :: limited-iata-code-list";
    }

    @GetMapping("/search")
    public Collection<ModelAndView> search(Model model, @ModelAttribute(SEARCH_TEXT_ATTRIBUTE_NAME) String searchText)  {
        long beforeSearch = System.currentTimeMillis();
        VoyagerListResponse<ResultSearch> voyagerResponse = voyagerAPI.lookup(searchText,0,Optional.of(5));
        List<ResultSearch> lookupResults = voyagerResponse.getResults();
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

    @GetMapping("/lookup")
    public String getSavedLocationOptions(Model model, @RequestParam Boolean isStart) {
        List<LocationDisplay> locations = voyagerAPI.getLocations();
        model.addAttribute("locations", locations);
        model.addAttribute("isStart", isStart);
        return "fragments/locations :: saved-locations-options";
    }

    @GetMapping("/review-location-reset")
    public Collection<ModelAndView> resetLocationReview(@RequestParam Boolean fromOrigin,
                                                        @RequestParam(required = false) Integer startLocationId,
                                                        @RequestParam(required = false) Integer endLocationId,
                                                        @RequestParam(required = false) String startAirportCode,
                                                        @RequestParam(required = false) String endAirportCode,
                                                        @RequestParam(required = false) String nonDeltaStartCode,
                                                        @RequestParam(required = false) String nonDeltaEndCode) {
        LocationDisplay startLocation = null;
        LocationDisplay endLocation = null;
        if (startLocationId != 0) startLocation = voyagerAPI.getLocationById(startLocationId);
        if (endLocationId != 0) endLocation = voyagerAPI.getLocationById(endLocationId);

        AirportDisplay startAirport = null;
        AirportDisplay endAirport = null;
        if (voyagerAPI.isDeltaIataCode(startAirportCode)) startAirport = voyagerAPI.getAirportByIata(startAirportCode).get();
        if (voyagerAPI.isDeltaIataCode(endAirportCode)) endAirport = voyagerAPI.getAirportByIata(endAirportCode).get();

        AirportDisplay nonDeltaStartAirport = null, nonDeltaEndAirport = null;
        if (voyagerAPI.isValidIataCode(nonDeltaStartCode)) nonDeltaStartAirport = voyagerAPI.getAirportByIata(nonDeltaStartCode).get();
        if (voyagerAPI.isValidIataCode(nonDeltaEndCode)) nonDeltaEndAirport = voyagerAPI.getAirportByIata(nonDeltaEndCode).get();

        ModelAndView reviewFragment = getUpdatedReviewPath(startLocation,endLocation,startAirport,endAirport,nonDeltaStartAirport,nonDeltaEndAirport);

        // reset closer non-delta airports
        List<AirportDisplay> civilList = new ArrayList<>();
        List<AirportDisplay> deltaList = new ArrayList<>();
        if (fromOrigin) {
            assert startLocation != null;
            civilList.addAll(voyagerAPI.nearbyAirports(startLocation.getLatitude(),startLocation.getLongitude(),10,Optional.of(AirportType.CIVIL),Optional.empty()));
            deltaList.addAll(voyagerAPI.nearbyAirports(startLocation.getLatitude(),startLocation.getLongitude(),5,Optional.empty(),Optional.of(Airline.DELTA)));
        } else {
            assert endLocation != null;
            civilList.addAll(voyagerAPI.nearbyAirports(endLocation.getLatitude(),endLocation.getLongitude(),10,Optional.of(AirportType.CIVIL),Optional.empty()));
            deltaList.addAll(voyagerAPI.nearbyAirports(endLocation.getLatitude(),endLocation.getLongitude(),5,Optional.empty(),Optional.of(Airline.DELTA)));
        }
        Set<String> deltaCodes = deltaList.stream().map(AirportDisplay::getIata).collect(Collectors.toSet());
        List<AirportDisplay> filtered = civilList.stream().filter(airportDisplay -> !deltaCodes.contains(airportDisplay.getIata())).toList();
        List<AirportDisplay> closer = new ArrayList<>();
        for (AirportDisplay airportDisplay : filtered) {
            if (airportDisplay.getDistance() >= deltaList.get(0).getDistance()) break;
            closer.add(airportDisplay);
        }
        closer = closer.stream().limit(5).toList();
        Map<String,Object> attributes = new HashMap<>(Map.of(
                "isStart",fromOrigin));
        if (!closer.isEmpty())  {
            attributes.put("nonDeltaAirportList",closer);
            attributes.put("closestAirportIata",closer.get(0).getIata());
        }
        ModelAndView nonDeltaMav = new ModelAndView("fragments/form :: non-delta-airport",attributes);
        return List.of(reviewFragment,nonDeltaMav);
    }

    public ModelAndView getNonDeltaMavForLngLat(Double longitude, Double latitude) {
        // check if closer non-delta airports exist
        List<AirportDisplay> civilList = new ArrayList<>(voyagerAPI.nearbyAirports(latitude,longitude,10,Optional.of(AirportType.CIVIL),Optional.empty()));
        List<AirportDisplay> deltaList = new ArrayList<>(voyagerAPI.nearbyAirports(latitude,longitude,10,Optional.empty(), Optional.of(Airline.DELTA)));

        Set<String> deltaCodes = deltaList.stream().map(AirportDisplay::getIata).collect(Collectors.toSet());
        List<AirportDisplay> filtered = civilList.stream().filter(airportDisplay -> !deltaCodes.contains(airportDisplay.getIata())).toList();
        List<AirportDisplay> closer = new ArrayList<>();
        for (AirportDisplay airportDisplay : filtered) {
            if (airportDisplay.getDistance() >= deltaList.get(0).getDistance()) break;
            closer.add(airportDisplay);
        }
        closer = closer.stream().limit(5).toList();
        Map<String,Object> attributes = new HashMap<>();
        if (!closer.isEmpty())  {
            attributes.put("nonDeltaAirportList",closer);
            attributes.put("closestAirportIata",closer.get(0).getIata());
        }
        return new ModelAndView("fragments/form :: non-delta-airport-not-trips",attributes);
    }

    public ModelAndView getUpdatedReviewPath(LocationDisplay startLocation, LocationDisplay endLocation, AirportDisplay startAirport, AirportDisplay endAirport, AirportDisplay nonDeltaStartAirport, AirportDisplay nonDeltaEndAirport) {
        Map<String,Object> attributes = new HashMap<>();
        if (startLocation != null) attributes.put("startLocation", startLocation);
        if (endLocation != null) attributes.put("endLocation", endLocation);
        if (startAirport != null) attributes.put("startAirport", startAirport);
        if (endAirport != null) attributes.put("endAirport", endAirport);
        if (nonDeltaStartAirport != null) attributes.put("nonDeltaStartAirport",nonDeltaStartAirport);
        if (nonDeltaEndAirport != null) attributes.put("nonDeltaEndAirport",nonDeltaEndAirport);

        if (startAirport != null && endAirport != null) {
            PathDisplay pathDisplay = voyagerAPI.getPath(startAirport.getIata(),endAirport.getIata());
            List<String> middleAirports = new ArrayList<>();
            for (RouteDisplay routeDisplay : pathDisplay.getRouteDisplayList()) {
                middleAirports.add(routeDisplay.getDestination());
            }
            middleAirports.remove(endAirport.getIata());
            attributes.put("middleAirports",middleAirports);
        }

        return new ModelAndView("fragments/trips :: review-path", attributes);
    }

    @GetMapping("/review-airport-reset")
    public Collection<ModelAndView> resetAirportReview(@RequestParam(required = false) String startAirportCode,
                                                       @RequestParam(required = false) String endAirportCode,
                                                       @RequestParam(required = false) Integer startLocationId,
                                                       @RequestParam(required = false) Integer endLocationId,
                                                       @RequestParam(required = false) String nonDeltaStartCode,
                                                       @RequestParam(required = false) String nonDeltaEndCode) {
        LocationDisplay startLocation = null, endLocation = null;
        if (startLocationId != 0) startLocation = voyagerAPI.getLocationById(startLocationId);
        if (endLocationId != 0) endLocation = voyagerAPI.getLocationById(endLocationId);

        AirportDisplay startAirport = null, endAirport = null;
        if (voyagerAPI.isDeltaIataCode(startAirportCode)) startAirport = voyagerAPI.getAirportByIata(startAirportCode).get();
        else if (voyagerAPI.isValidIataCode(startAirportCode) && StringUtils.isBlank(nonDeltaStartCode)) nonDeltaStartCode = startAirportCode;
        if (voyagerAPI.isDeltaIataCode(endAirportCode)) endAirport = voyagerAPI.getAirportByIata(endAirportCode).get();
        else if (voyagerAPI.isValidIataCode(endAirportCode) && StringUtils.isBlank(nonDeltaEndCode)) nonDeltaEndCode = endAirportCode;

        AirportDisplay nonDeltaStartAirport = null, nonDeltaEndAirport = null;
        if (voyagerAPI.isValidIataCode(nonDeltaStartCode)) nonDeltaStartAirport = voyagerAPI.getAirportByIata(nonDeltaStartCode).get();
        if (voyagerAPI.isValidIataCode(nonDeltaEndCode)) nonDeltaEndAirport = voyagerAPI.getAirportByIata(nonDeltaEndCode).get();

        return List.of(getUpdatedReviewPath(startLocation,endLocation,startAirport,endAirport, nonDeltaStartAirport, nonDeltaEndAirport));
    }

    @GetMapping("/test")
    public String testPage() {
        return "test";
    }
}
