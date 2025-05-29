package org.voyager.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.model.Airline;
import org.voyager.model.AirportCodes;
import org.voyager.model.AirportFilter;
import org.voyager.model.airport.Airport;
import org.voyager.model.airport.AirportType;
import org.voyager.model.location.Location;
import org.voyager.model.location.LocationForm;
import org.voyager.service.VoyagerService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.voyager.utils.ConstantsUI.AIRPORT_FILTER_PARAM_NAME;

@Controller
public class BrowseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowseController.class);
    @Autowired
    VoyagerService voyagerService;

    @GetMapping("/pin-airport")
    public String pinAirport(Model model, String airportCode, Integer iterIndex, @ModelAttribute AirportCodes airportCodes) {
        if (voyagerService.isValidIataCode(airportCode) && !airportCodes.getCodes().contains(airportCode)) airportCodes.getCodes().add(airportCode);
        List<Airport> airportList = airportCodes.getCodes().stream()
                .map(iata -> voyagerService.getAirport(iata)).toList();
        model.addAttribute("airportCodes", airportCodes);
        model.addAttribute("airportList", airportList);
        model.addAttribute("iterIndex",iterIndex);
        return "fragments/browse :: pinned-airport-section";
    }

    @GetMapping("/unpin-airport")
    public String unpinAirport(Model model, String airportCode, Integer iterIndex, @ModelAttribute AirportCodes airportCodes) {
        if (voyagerService.isValidIataCode(airportCode)) airportCodes.getCodes().remove(airportCode);
        List<Airport> airportList = airportCodes.getCodes().stream()
                .map(iata -> voyagerService.getAirport(iata)).toList();
        model.addAttribute("airportCodes", airportCodes);
        model.addAttribute("airportList", airportList);
        model.addAttribute("iterIndex",iterIndex);
        return "fragments/browse :: pinned-airport-section";
    }

    @PostMapping("/locations")
    public String addLocation(Model model, @ModelAttribute @Valid LocationForm locationForm, BindingResult bindingResult, @ModelAttribute AirportCodes airportCodes) {
        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error -> {
                if (error instanceof FieldError fieldError)LOGGER.error(String.format("'%s' %s",fieldError.getField(),fieldError.getDefaultMessage()));
                else LOGGER.error(error.getDefaultMessage());
            });
            model.addAttribute("locationForm",locationForm);
            return "fragments/form :: add-form-error";
        }
        try{
            locationForm.setAirports(airportCodes.getCodes());
            Location saved = voyagerService.addLocation(locationForm);
            LOGGER.info("saved: " + saved);
            model.addAttribute("locationForm", locationForm);
            return "fragments/form :: add-form-success";
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,e.getMessage(),e);
        }
    }

    @GetMapping("/nearby-airports")
    @Cacheable("nearbyAirportsCache")
    public String nearbyAirports(Model model, @RequestParam Double latitude, @RequestParam Double longitude,
                                 @RequestParam(AIRPORT_FILTER_PARAM_NAME) AirportFilter airportFilter,
                                 @ModelAttribute AirportCodes airportCodes) {
        LOGGER.info("nearbyAirports called with airportCodes: "+ airportCodes +" latitude: " + latitude + ", longitude: " + longitude);
        List<Airport> nearbyAirports = new ArrayList<>();
        switch (airportFilter) {
            case DELTA -> nearbyAirports.addAll(voyagerService.nearbyAirports(latitude,longitude,5, Airline.DELTA));
            case CIVIL -> nearbyAirports.addAll(voyagerService.nearbyAirports(latitude,longitude,5, AirportType.CIVIL));
            case MILITARY -> nearbyAirports.addAll(voyagerService.nearbyAirports(latitude,longitude,5,AirportType.MILITARY));
            case ALL -> nearbyAirports.addAll(voyagerService.nearbyAirports(latitude,longitude,5,AirportType.CIVIL));
        }
        model.addAttribute("airportList",nearbyAirports);
        model.addAttribute("pinnedCodeList",airportCodes.getCodes());
        return "fragments/options :: limited-iata-code-list";
    }



    @GetMapping("/nearby-airports-location")
    @Cacheable("nearbyAirportsCache")
    public String nearbyAirports(Model model, @RequestParam Integer locationId,
                                 @RequestParam(AIRPORT_FILTER_PARAM_NAME) AirportFilter airportFilter) {
        LOGGER.debug("nearbyAirports called with locationId: "+ locationId);
        Location location = voyagerService.getLocation(locationId);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        List<Airport> nearbyAirports = new ArrayList<>();
        switch (airportFilter) {
            case DELTA -> nearbyAirports.addAll(voyagerService.nearbyAirports(latitude,longitude,5, Airline.DELTA));
            case CIVIL -> nearbyAirports.addAll(voyagerService.nearbyAirports(latitude,longitude,5, AirportType.CIVIL));
            case MILITARY -> nearbyAirports.addAll(voyagerService.nearbyAirports(latitude,longitude,5,AirportType.MILITARY));
            case ALL -> nearbyAirports.addAll(voyagerService.nearbyAirports(latitude,longitude,5,AirportType.CIVIL));
        }
        model.addAttribute("airportList",nearbyAirports);
        model.addAttribute("pinnedCodeList",location.getAirports());
        return "fragments/options :: limited-iata-code-list";
    }
}
