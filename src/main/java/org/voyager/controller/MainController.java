package org.voyager.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestParam;
import org.voyager.model.AirportDisplay;
import org.voyager.model.ResultDisplay;
import org.voyager.model.TownDisplay;
import org.voyager.model.response.VoyagerListResponse;
import org.voyager.model.response.VoyagerResponseAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;
import org.voyager.service.VoyagerAPI;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Controller
public class MainController {

    @Autowired
    private VoyagerAPI voyagerAPI;

    @PostConstruct
    public void fetchAirportCodes() {
    }

    @GetMapping("/hello-world")
    public String helloWorld(Model model) {
        model.addAttribute("message","Hello World");
        return "hello-world";
    }

    @GetMapping("/")
    public String homepage(Model model) {
        VoyagerResponseAPI<TownDisplay> voyagerResponse = voyagerAPI.towns();
        model.addAttribute("towns",voyagerResponse.getResponseList());
        return "index";
    }

    @GetMapping("/general")
    public String generalPage() {
        return "general";
    }

    @GetMapping("/nearbyAirports")
    @Cacheable("nearbyAirportsCache")
    public String nearbyAirports(Model model, @RequestParam("iterIndex") Integer iterIndex, @RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude) {
        // TODO: update map here
        System.out.println("GET /nearbyAirports called with latitude: " + latitude + ", longitude: " + longitude);
        List<AirportDisplay> nearbyAirports = voyagerAPI.nearbyAirports(Double.parseDouble(latitude),Double.parseDouble(longitude),10);
        model.addAttribute("nearbyAirports",nearbyAirports);
        model.addAttribute("iterIndex",iterIndex);
        return "fragments/result-display :: iataCodeList";
    }

    @GetMapping("/search")
    @Cacheable("searchCache")
    public Collection<ModelAndView> search(Model model, @ModelAttribute("searchText") String searchText)  {
        long beforeSearch = System.currentTimeMillis();
        VoyagerListResponse<ResultDisplay> voyagerResponse = voyagerAPI.lookup(searchText,0);
        List<ResultDisplay> lookupResults = voyagerResponse.getResults();
        Integer totalResultsCount = voyagerResponse.getResultCount();
        double duration = (System.currentTimeMillis() - beforeSearch)/1000.0;
        System.out.println("duration of search: " + duration + "s");
        System.out.println("retrieved [" + lookupResults.size() + "] of [" + totalResultsCount + "] lookup results");
        return List.of(
                new ModelAndView("fragments/search-results :: results",
                        Map.of("lookupResults", lookupResults)),
                new ModelAndView("fragments/searchresults :: lookupFooterResults",
                        Map.of("totalResultsCount",totalResultsCount)));
    }

    @GetMapping("/test")
    public String testPage() {
        return "test";
    }

    @GetMapping("/lookup")
    @Cacheable("lookupCache")
    public Collection<ModelAndView> lookup(Model model, @ModelAttribute("searchText") String searchText)  {
        long beforeSearch = System.currentTimeMillis();
        VoyagerListResponse<ResultDisplay> voyagerResponse = voyagerAPI.lookup(searchText,0);
        List<ResultDisplay> lookupResults = voyagerResponse.getResults();
        Integer totalResultsCount = voyagerResponse.getResultCount();
        double duration = (System.currentTimeMillis() - beforeSearch)/1000.0;
        System.out.println("duration of search: " + duration + "s");
        System.out.println("retrieved [" + lookupResults.size() + "] of [" + totalResultsCount + "] lookup results");
        return List.of(
                new ModelAndView("fragments/search-results :: results",
                        Map.of("lookupResults", lookupResults)),
                new ModelAndView("fragments/searchresults :: lookupFooterResults",
                        Map.of("totalResultsCount",totalResultsCount)));
    }
}
