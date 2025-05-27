package org.voyager.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.model.AirportCodes;
import org.voyager.model.airport.Airport;
import org.voyager.model.location.Location;
import org.voyager.model.location.LocationForm;
import org.voyager.service.VoyagerService;

import java.util.HashSet;
import java.util.List;

@Controller
public class BrowseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowseController.class);
    @Autowired
    VoyagerService voyagerService;

    @GetMapping("/pin-airport")
    public String pinAirport(Model model, String airportCode, Integer iterIndex, @ModelAttribute AirportCodes airportCodes) {
        if (voyagerService.isValidIataCode(airportCode)) airportCodes.getCodes().add(airportCode);
        // TODO: returns pinned airport and udpates some value that tracks list of airports
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
        // TODO: returns pinned airport and udpates some value that tracks list of airports
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
            locationForm.setAirports(new HashSet<>(airportCodes.getCodes()));
            Location saved = voyagerService.addLocation(locationForm);
            LOGGER.info("saved: " + saved);
            model.addAttribute("locationForm", locationForm);
            return "fragments/form :: add-form-success";
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,e.getMessage(),e);
        }
    }
}
