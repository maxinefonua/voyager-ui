package org.voyager.controller;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.model.*;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.location.Location;
import org.voyager.model.response.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;
import org.voyager.service.VoyagerService;
import org.voyager.service.impl.SearchServiceAPI;

import java.util.*;
import java.util.stream.Collectors;

import static org.voyager.utils.ConstantsUI.*;

@Controller
public class MainController {
    private static final DefaultPage DEFAULT_PAGE = DefaultPage.TRIPS;

    @Autowired
    private VoyagerService voyagerService;

    @Autowired
    private TripsController tripsController;

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    @GetMapping("/hello-world")
    public String helloWorld(Model model) {
        model.addAttribute("message","Hello World");
        return "hello-world";
    }

    @GetMapping("/")
    public String homepage(Model model) {
        try {
            model.addAttribute("defaultPage", DEFAULT_PAGE);
            switch (DEFAULT_PAGE) {
                case TRIPS -> {
                    tripsController.addDefaultAttributes(model);
                }
            }
            return "index";
        } catch (ResponseStatusException e) {
            LOGGER.error("APIs are down, returning down page");
            return "down";
        }
    }

    @GetMapping("/general")
    public String generalPage() {
        return "general";
    }

//    @GetMapping("/reverse-trip")
//    @Cacheable("reverseCache")
//    public Collection<ModelAndView> reverseTrip(@RequestParam(required = false) String startAirportCode,
//                                                @RequestParam(required = false) String endAirportCode,
//                                                @RequestParam(required = false) Integer startLocationId,
//                                                @RequestParam(required = false) Integer endLocationId,
//                                                @RequestParam(required = false) String nonDeltaStartCode,
//                                                @RequestParam(required = false) String nonDeltaEndCode) {
//        return resetAirportReview(endAirportCode,startAirportCode,endLocationId,startLocationId,nonDeltaEndCode,nonDeltaStartCode);
//    }

//    @GetMapping("/review-airport-reset")
//    public Collection<ModelAndView> resetAirportReview(@RequestParam(required = false) String startAirportCode,
//                                                       @RequestParam(required = false) String endAirportCode,
//                                                       @RequestParam(required = false) Integer startLocationId,
//                                                       @RequestParam(required = false) Integer endLocationId,
//                                                       @RequestParam(required = false) String nonDeltaStartCode,
//                                                       @RequestParam(required = false) String nonDeltaEndCode) {
//        Location startLocation = null, endLocation = null;
//        if (startLocationId != 0) startLocation = voyagerService.getLocation(startLocationId);
//        if (endLocationId != 0) endLocation = voyagerService.getLocation(endLocationId);
//
//        Airport startAirport = null, endAirport = null;
//        if (voyagerService.isDeltaIataCode(startAirportCode)) startAirport = voyagerService.getAirport(startAirportCode);
//        else if (voyagerService.isValidIataCode(startAirportCode) && StringUtils.isBlank(nonDeltaStartCode)) nonDeltaStartCode = startAirportCode;
//        if (voyagerService.isDeltaIataCode(endAirportCode)) endAirport = voyagerService.getAirport(endAirportCode);
//        else if (voyagerService.isValidIataCode(endAirportCode) && StringUtils.isBlank(nonDeltaEndCode)) nonDeltaEndCode = endAirportCode;
//
//        Airport nonDeltaStartAirport = null, nonDeltaEndAirport = null;
//        if (voyagerService.isValidIataCode(nonDeltaStartCode)) nonDeltaStartAirport = voyagerService.getAirport(nonDeltaStartCode);
//        if (voyagerService.isValidIataCode(nonDeltaEndCode)) nonDeltaEndAirport = voyagerService.getAirport(nonDeltaEndCode);
//
//        return List.of(getUpdatedReviewPath(startLocation,endLocation,startAirport,endAirport, nonDeltaStartAirport, nonDeltaEndAirport));
//    }

    @GetMapping("/test")
    public String testPage() {
        return "test";
    }

    @GetMapping("/about")
    public String aboutPage() {
        return "about";
    }
}
