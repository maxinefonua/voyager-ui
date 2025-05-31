package org.voyager.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.voyager.model.Airline;
import org.voyager.model.Option;
import org.voyager.model.TripFilter;
import org.voyager.model.location.Location;
import org.voyager.model.location.Status;
import org.voyager.service.VoyagerService;

import java.util.ArrayList;
import java.util.List;

@Controller
public class TripsController {

    private static final TripFilter DEFAULT_TRIP_FILTER = TripFilter.LOCATION;

    @Autowired
    private VoyagerService voyagerService;

    void addDefaultAttributes(Model model) {
        model.addAttribute("selection",DEFAULT_TRIP_FILTER.name());
        model.addAttribute("filterList",Option.getFilterOptions(DEFAULT_TRIP_FILTER));
        model.addAttribute("optionList",getOptionsList(DEFAULT_TRIP_FILTER));
    }

    @GetMapping("/from-selection")
    public String selectFrom(Model model, @RequestParam TripFilter tripFilter) {
        model.addAttribute("filterList",Option.getFilterOptions(tripFilter));
        model.addAttribute("optionList",getOptionsList(tripFilter));
        model.addAttribute("selection",tripFilter.name());
        return "fragments/routes :: selected-start-locations";
    }

    @GetMapping("/trips")
    public String getTrips(Model model) {
        List<Location> locations = voyagerService.getLocations();
        model.addAttribute("locations",locations);
        model.addAttribute("lookupAttribution", voyagerService.lookupAttribution());
        addDefaultAttributes(model);

        return "fragments/tab :: trips-tab";
    }

    private List<Option> getOptionsList(TripFilter filter) {
        List<Option> datalistOptions = new ArrayList<>();
        switch (filter) {
            case AIRPORT -> {
                datalistOptions = voyagerService.airports(Airline.DELTA).stream()
                        .map(airport -> Option.builder().elementName(airport.getIata()).display(
                                        String.format("%s | %s, %s of %s", airport.getName(), airport.getCity(),
                                                airport.getSubdivision(),airport.getCountryCode()))
                                .value(airport.getIata()).build())
                        .toList();
            }
            case LOCATION -> {
                datalistOptions = voyagerService.getLocations(Status.SAVED).stream()
                        .map(location -> Option.builder().elementName(String.format("%s-%s",
                                        location.getName(),location.getId())).display(String.format("%s, %s in %s",
                                        location.getName(),location.getSubdivision(), location.getCountryCode()))
                                .value(String.valueOf(location.getId().intValue())).build())
                        .toList();
            }
        }
        return datalistOptions;
    }
}
