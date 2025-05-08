package org.voyager.controller;
import jakarta.validation.Valid;
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
import org.voyager.service.VoyagerAPI;
import org.voyager.validate.ValidationUtils;

import java.util.*;

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
        return "fragments/locations :: all-airports";
    }

    @GetMapping("/trips")
    public String getTrips(Model model) {
        List<LocationDisplay> locations = voyagerAPI.getLocations();
        model.addAttribute("locations",locations);
        model.addAttribute("lookupAttribution", voyagerAPI.lookupAttribution());
        return "fragments/tab :: trips-tab";
    }

    @GetMapping("/nearby-airports-old")
    @Cacheable("nearbyAirportsCache")
    public Collection<ModelAndView> nearbyAirports2(Model model, @RequestParam Integer iterIndex, @RequestParam Double latitude, @RequestParam Double longitude, @RequestParam(AIRPORT_FILTER_PARAM_NAME) Optional<String> filterOptional) {
        AirportFilter airportFilter = ValidationUtils.resolveAirportFilterOptional(filterOptional);
        Optional<AirportType> type = Optional.empty();
        Optional<Airline> airline = Optional.empty();
        switch (airportFilter) {
            case DELTA -> airline = Optional.of(Airline.DELTA);
            case CIVIL -> type = Optional.of(AirportType.CIVIL);
            case MILITARY -> type = Optional.of(AirportType.MILITARY);
        }
        List<AirportDisplay> nearbyAirports = voyagerAPI.nearbyAirports(latitude,longitude,5,type,airline);
        return List.of(
                new ModelAndView("fragments/result-display :: iata-code-list",
                        Map.of("nearbyAirports", nearbyAirports,
                                "iterIndex",iterIndex,
                                "latitude",latitude,
                                "longitude",longitude)));

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
        model.addAttribute("nearbyAirports",nearbyAirports);
        model.addAttribute("latitude",latitude);
        model.addAttribute("longitude",longitude);
        return "fragments/result-display :: iata-code-list";
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

    @GetMapping("/review")
    public String updateTripReview(Model model, @RequestParam Boolean fromOrigin, @RequestParam Integer selectedId, @RequestParam(required = false) String airportCode) {
        LocationDisplay locationDisplay = voyagerAPI.getLocationById(selectedId);
        if (fromOrigin) {
            model.addAttribute("startLocation",locationDisplay.getName());
            model.addAttribute("origin",airportCode);
            return "fragments/trips :: review-from";
        }
        model.addAttribute("endLocation",locationDisplay.getName());
        model.addAttribute("destination",airportCode);
        return "fragments/trips :: review-to";
    }

    @GetMapping("/test")
    public String testPage() {
        return "test";
    }
}
