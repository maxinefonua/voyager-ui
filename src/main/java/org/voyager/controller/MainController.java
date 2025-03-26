package org.voyager.controller;

import org.voyager.model.ResultDisplay;
import org.voyager.model.TownDisplay;
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

    @GetMapping("/test")
    public String testPage() {
        return "test";
    }

    @GetMapping("/lookup")
    public Collection<ModelAndView> lookup(Model model, @ModelAttribute("searchText") String searchText)  {
        long beforeSearch = System.currentTimeMillis();
        VoyagerResponseAPI<ResultDisplay> voyagerResponse = voyagerAPI.lookup(searchText,0);
        List<ResultDisplay> lookupResults = voyagerResponse.getResponseList();
        Integer totalResultsCount = voyagerResponse.getTotalResponseCount();
        double duration = (System.currentTimeMillis() - beforeSearch)/1000.0;
        System.out.println("duration of search: " + duration + "s");
        System.out.println("retrieved [" + lookupResults.size() + "] of [" + totalResultsCount + "] lookup results");
        return List.of(
                new ModelAndView("fragments/searchresults :: searchresults",
                        Map.of("lookupResults", lookupResults)),
                new ModelAndView("fragments/searchresults :: lookupFooterResults",
                        Map.of("totalResultsCount",totalResultsCount)));
    }
}
