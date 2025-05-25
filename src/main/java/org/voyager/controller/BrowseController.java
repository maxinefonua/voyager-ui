package org.voyager.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.voyager.model.airport.Airport;
import org.voyager.model.location.Location;
import org.voyager.service.VoyagerService;

import java.util.List;

@Controller
public class BrowseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowseController.class);
    @Autowired
    VoyagerService voyagerService;

    @GetMapping("/pin-airport")
    public String pinAirport(Model model, String airportCode) {
        Airport airport = voyagerService.getAirport(airportCode.toUpperCase());
        model.addAttribute("airport",airport);
        LOGGER.debug(String.format("pinning %s",airport));
        return "fragments/locations :: pinned-airport";
    }

    @GetMapping("/unpin-airport")
    public String unpinAirport(String airportCode) {
        Airport airport = voyagerService.getAirport(airportCode.toUpperCase());
        LOGGER.debug(String.format("pinning %s",airport));
        return "fragments/locations :: pinned-airport";
    }
}
