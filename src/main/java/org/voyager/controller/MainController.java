package org.voyager.controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.voyager.model.*;
import org.voyager.model.response.VoyagerListResponse;
import org.voyager.model.response.VoyagerResponseAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;
import org.voyager.model.result.ResultSearch;
import org.voyager.service.VoyagerAPI;
import org.voyager.validate.ValidationUtils;

import java.util.*;

import static org.voyager.utils.ConstantsUI.AIRPORT_FILTER_PARAM_NAME;
import static org.voyager.utils.ConstantsUtils.AIRLINE_PARAM_NAME;
import static org.voyager.utils.ConstantsUtils.TYPE_PARAM_NAME;

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
        VoyagerResponseAPI<TownDisplay> voyagerResponse = voyagerAPI.towns();
        model.addAttribute("towns",voyagerResponse.getResponseList());
        model.addAttribute("lookupAttribution", voyagerAPI.lookupAttribution());
        return "index";
    }

    @GetMapping("/general")
    public String generalPage() {
        return "general";
    }

    @GetMapping("/saved")
    public Collection<ModelAndView> getSaved() {
        VoyagerResponseAPI<TownDisplay> voyagerResponse = voyagerAPI.towns();
        return List.of(
                new ModelAndView("fragments/tab :: savedTab",
                        Map.of("towns",voyagerResponse.getResponseList())),
                new ModelAndView("fragments/tab :: tab1Active"));
    }

    @GetMapping("/add")
    public Collection<ModelAndView> getAdd() {
        return List.of(
                new ModelAndView( "fragments/tab :: addTab",
                        Map.of("lookupAttribution", voyagerAPI.lookupAttribution())),
                new ModelAndView("fragments/tab :: tab2Active"));
    }

    @PostMapping("/locations")
    public String addLocation(@ModelAttribute LocationForm locationForm, Model model) {
        model.addAttribute("locationForm", locationForm);
        return "fragments/form :: add-form-success";
    }

    @GetMapping("/nearby-airports")
    @Cacheable("nearbyAirportsCache")
    public Collection<ModelAndView> nearbyAirports(Model model, @RequestParam Integer iterIndex, @RequestParam Double latitude, @RequestParam Double longitude, @RequestParam(AIRPORT_FILTER_PARAM_NAME) Optional<String> filterOptional) {
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
                                "longitude",longitude)),
                new ModelAndView("fragments/result-display :: iata-code-input",
                        Map.of("iterIndex",iterIndex,
                                "firstAirportCode",nearbyAirports.get(0).getIata())));

    }

    @GetMapping("/search")
    @Cacheable("searchCache")
    public Collection<ModelAndView> search(Model model, @ModelAttribute("searchText") String searchText)  {
        long beforeSearch = System.currentTimeMillis();
        VoyagerListResponse<ResultSearch> voyagerResponse = voyagerAPI.lookup(searchText,0);
        List<ResultSearch> lookupResults = voyagerResponse.getResults();
        Integer totalResultsCount = voyagerResponse.getResultCount();
        double duration = (System.currentTimeMillis() - beforeSearch)/1000.0;
        LOGGER.info("duration of search: " + duration + "s");
        LOGGER.info("retrieved [" + lookupResults.size() + "] of [" + totalResultsCount + "] lookup results");
        return List.of(
                new ModelAndView("fragments/search :: accordionResults",
                        Map.of("lookupResults", lookupResults,
                                "searchText",searchText)),
                new ModelAndView("fragments/search :: lookupFooterResults",
                        Map.of("totalResultsCount",totalResultsCount)));
    }

    @GetMapping("/test")
    public String testPage() {
        return "test";
    }
}
