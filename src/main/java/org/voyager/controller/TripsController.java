package org.voyager.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.voyager.model.Airline;
import org.voyager.model.Option;
import org.voyager.model.TripFilter;
import org.voyager.model.location.Status;
import org.voyager.service.VoyagerService;

import java.util.ArrayList;
import java.util.List;

@Controller
public class TripsController {

    @Autowired
    private VoyagerService voyagerService;

    @GetMapping("/from-selection")
    public String selectFrom(Model model, @RequestParam TripFilter tripFilter) {
//        selection,filterList,optionList
        model.addAttribute("filterList",Option.getFilterOptions(tripFilter));
        List<Option> datalistOptions = new ArrayList<>();
        switch (tripFilter) {
            case AIRPORT -> {
                model.addAttribute("optionList",
                        voyagerService.airports(Airline.DELTA).stream().map(airport ->
                        Option.builder().display(String.format("%s | %s, %s of %s", airport.getName(),
                                        airport.getCity(),airport.getSubdivision(),airport.getCountryCode()))
                                .value(airport.getIata()).build())
                        .toList()
                );
            }
            case LOCATION -> {
                model.addAttribute("optionList",
                        voyagerService.getLocations(Status.SAVED).stream().map(location ->
                                        Option.builder().display(String.format("%s, %s in %s",
                                                                location.getName(),location.getSubdivision(),
                                                                location.getCountryCode()))
                                                .value(String.valueOf(location.getId().intValue())).build()).toList()
                );
            }
        }
        model.addAttribute("selection",tripFilter.name());
        return "fragments/routes :: selected-start-locations";
    }
}
