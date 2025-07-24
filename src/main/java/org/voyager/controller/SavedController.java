package org.voyager.controller;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.voyager.config.VoyagerAPIConfig;
import org.voyager.model.LocationDetails;
import org.voyager.model.LocationFilter;
import org.voyager.model.Option;
import org.voyager.model.airport.Airport;
import org.voyager.model.country.Continent;
import org.voyager.model.country.Country;
import org.voyager.model.location.Location;
import org.voyager.model.location.LocationPatch;
import org.voyager.model.location.Source;
import org.voyager.model.location.Status;
import org.voyager.service.CountryService;
import org.voyager.service.Voyager;
import org.voyager.service.VoyagerService;
import org.voyager.service.impl.CountryServiceAPI;
import org.voyager.service.impl.LocationServiceAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Controller
public class SavedController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SavedController.class);
    private static CountryServiceAPI countryServiceAPI;
    private static LocationServiceAPI locationServiceAPI;

    @Autowired
    VoyagerService voyagerService;

    @PostConstruct
    public void init() {
        countryServiceAPI = voyagerService.getCountryServiceAPI();
        locationServiceAPI = voyagerService.getLocationServiceAPI();
    }

    void addDefaultAttributes(Model model) {
        List<List<Country>> continentCountryList = new ArrayList<>();
        List<List<List<Location>>> continentLocationList = new ArrayList<>();
        Source source = Source.valueOf(voyagerService.lookupAttribution().getName().toUpperCase());
        for (Continent continent : Continent.values()) {
            List<String> countryCodes = new ArrayList<>();
            List<Country> countryList = new ArrayList<>();
            List<List<Location>> countryLocationsList = new ArrayList<>();
            List<Location> continentLocations = locationServiceAPI.getLocations(source,continent);
            continentLocations.forEach(location -> {
                String countryCode = location.getCountryCode();
                if (countryCodes.contains(countryCode)) {
                    countryLocationsList.get(countryCodes.indexOf(countryCode)).add(location);
                } else {
                    Country country = countryServiceAPI.getCountry(countryCode);
                    countryList.add(country);
                    countryList.sort(Comparator.comparing(Country::getName));
                    int insertIndex = countryList.indexOf(country);
                    countryCodes.add(insertIndex,countryCode);
                    countryLocationsList.add(insertIndex,new ArrayList<>(List.of(location)));
                }
            });
            continentCountryList.add(countryList);
        }
        model.addAttribute("continentList", Continent.values());
        model.addAttribute("continentCountryList", continentCountryList);
        model.addAttribute("continentLocationList", continentLocationList);

//            // TODO: add country details for airports
        List<Location> locations = voyagerService.getLocations(Status.SAVED);
        List<LocationDetails> locationDetailsList = new ArrayList<>();
        locations.forEach(location-> { // TODO: add country details for airports
            List<Airport> airportList = new ArrayList<>(location.getAirports().stream()
                    .map(iata -> voyagerService.getAirport(iata)).toList());
            locationDetailsList.add(LocationDetails.builder().airportList(airportList).location(location).build());
            location.setCountryCode(countryServiceAPI.getCountry(location.getCountryCode()).getCode());
        });
        model.addAttribute("locationDetailsList",locationDetailsList);
        model.addAttribute("locationFilter",new LocationFilter());
    }

    @GetMapping("/saved")
    public String getSaved(Model model) {
        addDefaultAttributes(model);
        return "fragments/tab :: saved-tab";
    }

    @GetMapping("/saved-locations")
    public String getSavedLocations(Model model,LocationFilter locationFilter) {
        List<Location> locations = voyagerService.getLocations();
        if (locationFilter.getFilterArchive()) {
            locations = locations.stream().filter(location -> !location.getStatus().equals(Status.ARCHIVED)).toList();
        }
        List<LocationDetails> locationDetailsList = new ArrayList<>();
        locations.forEach(location-> { // TODO: add country details for airports
            List<Airport> airportList = new ArrayList<>(location.getAirports().stream()
                    .map(iata -> voyagerService.getAirport(iata)).toList());
            locationDetailsList.add(LocationDetails.builder().airportList(airportList).location(location).build());
            location.setCountryCode(countryServiceAPI.getCountry(location.getCountryCode()).getName());
        });
        model.addAttribute("locationFilter",locationFilter);
        model.addAttribute("locationDetailsList",locationDetailsList);
        return "fragments/locations :: location-details";
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
        airportCode = airportCode.toUpperCase();
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

    @GetMapping("/location-status")
    public String pinAirportToLocation(Model model, @NonNull Status status, @NonNull Integer locationId) {
        Location location = voyagerService.getLocation(locationId);
        if (!location.getStatus().equals(status)) {
            LocationPatch locationPatch = LocationPatch.builder().status(status.name()).build();
            LOGGER.debug(String.format("patch request %s to location %s",locationPatch,location));
            Location patched = voyagerService.patchLocation(locationId,locationPatch);
            LOGGER.info(String.format("patched location %s",patched));
        }
        model.addAttribute("status",location.getStatus());
        return "fragments/form :: archive-location-success";
    }
}
