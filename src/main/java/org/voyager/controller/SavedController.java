package org.voyager.controller;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.voyager.model.LocationDetails;
import org.voyager.model.LocationFilter;
import org.voyager.model.airport.Airport;
import org.voyager.model.country.Continent;
import org.voyager.model.country.Country;
import org.voyager.model.location.Location;
import org.voyager.model.location.LocationPatch;
import org.voyager.model.location.Source;
import org.voyager.model.location.Status;
import org.voyager.service.VoyagerService;
import org.voyager.service.impl.CountryServiceAPI;
import org.voyager.service.impl.LocationServiceAPI;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SavedController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SavedController.class);
    private static CountryServiceAPI countryServiceAPI;
    private static LocationServiceAPI locationServiceAPI;
    private Source source;

    @Autowired
    private VoyagerService voyagerService;

    @PostConstruct
    public void init() {
        countryServiceAPI = voyagerService.getCountryServiceAPI();
        locationServiceAPI = voyagerService.getLocationServiceAPI();
        source = Source.valueOf(voyagerService.lookupAttribution().getName().toUpperCase());
    }

    void addDefaultAttributes(Model model) {
        List<List<Country>> continentCountryList = new ArrayList<>();
        List<List<List<Location>>> continentCountryLocationsList = new ArrayList<>();
        for (Continent continent : Continent.values()) {
            List<Country> countryList = new ArrayList<>();
            List<List<Location>> countryLocationsList = new ArrayList<>();
            List<Location> continentLocations = locationServiceAPI.getLocations(source,continent,List.of(Status.SAVED));
            Map<String,List<Location>> locationListGroupedByCountryCode = continentLocations.stream()
                            .collect(Collectors.groupingBy(Location::getCountryCode));
            locationListGroupedByCountryCode.forEach((countryCode,countryLocations) -> {
                Country country = countryServiceAPI.getCountry(countryCode);
                countryList.add(country);
                countryLocations.sort(Comparator.comparing(Location::getName));
                countryLocationsList.add(countryLocations);
            });
            countryList.sort(Comparator.comparing(Country::getName));
            continentCountryList.add(countryList);
            continentCountryLocationsList.add(countryLocationsList);
        }
        model.addAttribute("continentList", Continent.values());
        model.addAttribute("continentCountryList", continentCountryList);
        model.addAttribute("continentCountryLocationsList", continentCountryLocationsList);
        model.addAttribute("locationFilter",new LocationFilter());
    }

    @GetMapping("/saved")
    public String getSaved(Model model) {
        addDefaultAttributes(model);
        model.addAttribute("locationFilter",new LocationFilter());
        return "fragments/tab :: saved-tab";
    }

    @GetMapping("/saved-locations")
    public String getSavedLocations(Model model,@ModelAttribute LocationFilter locationFilter) {
        List<List<Country>> continentCountryList = new ArrayList<>();
        List<List<List<Location>>> continentCountryLocationsList = new ArrayList<>();
        Source source = Source.valueOf(voyagerService.lookupAttribution().getName().toUpperCase());
        for (Continent continent : Continent.values()) {
            List<String> countryCodes = new ArrayList<>();
            List<Country> countryList = new ArrayList<>();
            List<List<Location>> countryLocationsList = new ArrayList<>();
            List<Location> continentLocations = locationServiceAPI.getLocations(source,continent,
                    locationFilter.getIncludeArchived()?List.of(Status.SAVED,Status.ARCHIVED):List.of(Status.SAVED));
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
            continentCountryLocationsList.add(countryLocationsList);
        }
        model.addAttribute("locationFilter",locationFilter);
        model.addAttribute("continentList", Continent.values());
        model.addAttribute("continentCountryList", continentCountryList);
        model.addAttribute("continentCountryLocationsList", continentCountryLocationsList);
        return "fragments/saved :: main-saved-page";
    }

    @GetMapping("/unpin-airport-location")
    public String unpinAirport(Model model, @NonNull String airportCode, @NonNull Integer locationId) {
        LOGGER.debug(String.format("/unpin-airport-location called from locationId %d and airportCode %s",
                locationId, airportCode));
        Location location = locationServiceAPI.getLocation(locationId);
        if (voyagerService.isValidIataCode(airportCode) && location.hasAirport(airportCode)) {
            location.removeAirport(airportCode);
            LocationPatch locationPatch = LocationPatch.builder().airports(location.getAirports()).build();
            LOGGER.debug(String.format("patch request %s to location %s", locationPatch, location));
            Location patched = locationServiceAPI.patchLocation(locationId, locationPatch);
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
        Location location = locationServiceAPI.getLocation(locationId);
        if (voyagerService.isValidIataCode(airportCode) && !location.hasAirport(airportCode)) {
            location.addAirport(airportCode);
            LocationPatch locationPatch = LocationPatch.builder().airports(location.getAirports()).build();
            LOGGER.debug(String.format("patch request %s to location %s",locationPatch,location));
            Location patched = locationServiceAPI.patchLocation(locationId,locationPatch);
            model.addAttribute("location",patched);
            Airport airport = voyagerService.getAirport(airportCode);
            model.addAttribute("airport",airport);
        } else
            model.addAttribute("location",location);
        return "fragments/pinned :: pinned-airport";
    }

    @GetMapping("/status-button")
    public String getStatusButton(Model model, @NonNull LocationFilter locationFilter,
                                  @NonNull Integer locationId, @NonNull Status status) {
        model.addAttribute("locationFilter",locationFilter);
        model.addAttribute("locationId",locationId);
        model.addAttribute("locationStatusName",status.name());
        return "fragments/saved :: status-update-button";
    }

    @GetMapping("/location-status")
    public String updateLocationStatus(Model model, @NonNull Status status,
                                       @NonNull Integer locationId,
                                       @NonNull LocationFilter locationFilter) {
        if (status.equals(Status.DELETE)) {
            Boolean deleted = locationServiceAPI.deleteLocation(locationId);
            if (deleted)
                LOGGER.info(String.format("successfully deleted location with id: %d",locationId));
            else LOGGER.error(String.format("error deleting location with id: %d",locationId));
        } else {
            Location location = locationServiceAPI.getLocation(locationId);
            if (!location.getStatus().equals(status)) {
                LocationPatch locationPatch = LocationPatch.builder().status(status.name()).build();
                LOGGER.debug(String.format("patch request %s to location %s", locationPatch, location));
                Location patched = locationServiceAPI.patchLocation(locationId, locationPatch);
                LOGGER.info(String.format("patched location %s", patched));
            } else {
                LOGGER.info(String.format("skipping location update of matching status: %s",location));
            }
        }
        model.addAttribute("locationStatusName",status.name());
        model.addAttribute("includeArchived",locationFilter.getIncludeArchived());
        model.addAttribute("locationId",locationId);
        return "fragments/saved :: update-location-status-success";
    }
}
