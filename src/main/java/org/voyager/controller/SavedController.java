package org.voyager.controller;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.voyager.model.airport.Airport;
import org.voyager.model.location.Location;
import org.voyager.model.location.LocationPatch;
import org.voyager.service.VoyagerService;

import java.util.List;

@Controller
public class SavedController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SavedController.class);

    @Autowired
    VoyagerService voyagerService;

    @GetMapping("/lookup")
    public String getSavedLocationOptions(Model model, @RequestParam Boolean isStart) {
        List<Location> locations = voyagerService.getLocations();
        model.addAttribute("locations", locations);
        model.addAttribute("isStart", isStart);
        return "fragments/locations :: saved-locations-options";
    }

    @GetMapping("/pinned-airport-display")
    public String fetchPinnedAirport(Model model, String airportCode, Integer locationId) {
        LOGGER.debug(String.format("/pinned-airport called from locationId %d and airportCode %s",
                locationId,airportCode));
        if (voyagerService.isValidIataCode(airportCode)) {
            Airport airport = voyagerService.getAirport(airportCode.toUpperCase());
            model.addAttribute("airport", airport); // button display
            model.addAttribute("locationId", locationId); // for unpin button
            LOGGER.debug(String.format("pinning %s", airport));
        }
        return "fragments/pinned :: pinned-airport-display";
    }

    @GetMapping("/unpin-airport-location")
    public String unpinAirport(Model model, @NonNull String airportCode, @NonNull Integer locationId) {
        LOGGER.debug(String.format("/unpin-airport-location called from locationId %d and airportCode %s",
                locationId, airportCode));
        Location location = voyagerService.getLocation(locationId);
        if (voyagerService.isValidIataCode(airportCode) && location.hasAirport(airportCode)) {
            location.removeAirport(airportCode);
            LocationPatch locationPatch = LocationPatch.builder().airports(location.getAirports()).build();
            LOGGER.debug(String.format("patch request %s to location %s", locationPatch, location));
            Location patched = voyagerService.patchLocation(locationId, locationPatch);
            model.addAttribute("location", patched);
        } else
            model.addAttribute("location", location);
        return "fragments/pinned :: pinned-airport";
    }

    @GetMapping("/pin-airport-location")
    public String pinAirportToLocation(Model model, @NonNull String airportCode, @NonNull Integer locationId) {
        LOGGER.debug(String.format("/pin-airport-location called from locationId %d and airportCode %s",
                locationId,airportCode));
        Location location = voyagerService.getLocation(locationId);
        if (voyagerService.isValidIataCode(airportCode) && !location.hasAirport(airportCode)) {
            location.addAirport(airportCode);
            LocationPatch locationPatch = LocationPatch.builder().airports(location.getAirports()).build();
            LOGGER.debug(String.format("patch request %s to location %s",locationPatch,location));
            Location patched = voyagerService.patchLocation(locationId,locationPatch);
            model.addAttribute("location",patched);
            Airport airport = voyagerService.getAirport(airportCode);
            model.addAttribute("airport",airport);
        } else
            model.addAttribute("location",location);
        return "fragments/pinned :: pinned-airport";
    }
}
