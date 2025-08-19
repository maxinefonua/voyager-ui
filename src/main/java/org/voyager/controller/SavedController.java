package org.voyager.controller;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.model.LocationFilter;
import org.voyager.model.airport.Airport;
import org.voyager.model.country.Continent;
import org.voyager.model.country.Country;
import org.voyager.model.currency.Currency;
import org.voyager.model.language.Language;
import org.voyager.model.location.Location;
import org.voyager.model.location.LocationPatch;
import org.voyager.model.location.Source;
import org.voyager.model.location.Status;
import org.voyager.service.VoyagerService;
import org.voyager.service.impl.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class SavedController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SavedController.class);
    private static CountryServiceAPI countryServiceAPI;
    private static LocationServiceAPI locationServiceAPI;
    private static SearchServiceAPI searchServiceAPI;
    private static CurrencyServiceAPI currencyServiceAPI;
    private static LanguageServiceAPI languageServiceAPI;

    private static String DEFAULT_BASE_CURRENCY = "USD";
    private Source source;

    @Autowired
    private VoyagerService voyagerService;

    @Autowired
    private TripsController tripsController;

    @PostConstruct
    public void init() {
        countryServiceAPI = voyagerService.getCountryServiceAPI();
        locationServiceAPI = voyagerService.getLocationServiceAPI();
        searchServiceAPI = voyagerService.getSearchServiceAPI();
        currencyServiceAPI = voyagerService.getCurrencyServiceAPI();
        languageServiceAPI = voyagerService.getLanguageServiceAPI();
        source = searchServiceAPI.getSource();
    }

    void addDefaultAttributes(Model model,LocationFilter locationFilter) {
        List<List<Country>> continentCountryList = new ArrayList<>();
        List<List<List<Location>>> continentCountryLocationsList = new ArrayList<>();
        Double minExchangeRateValue = Double.MAX_VALUE;
        Double maxExchangeRateValue = Double.MIN_VALUE;
        for (Continent continent : Continent.values()) {
            List<Country> countryList = new ArrayList<>();
            List<List<Location>> countryLocationsList = new ArrayList<>();
            List<Location> continentLocations = locationServiceAPI.getLocations(source,continent,
                    locationFilter.getIncludeArchived()?List.of(Status.SAVED,Status.ARCHIVED):List.of(Status.SAVED));

            Map<String,List<Location>> locationListGroupedByCountryCode = continentLocations.stream()
                            .collect(Collectors.groupingBy(Location::getCountryCode));
            for (Map.Entry<String,List<Location>> entry : locationListGroupedByCountryCode.entrySet()) {
                String countryCode = entry.getKey();
                List<Location> countryLocations = entry.getValue();
                Country country = countryServiceAPI.getCountry(countryCode);
                Currency countryCurrency = currencyServiceAPI.getCurrency(country.getCurrencyCode());
                List<String> languageNames = country.getLanguages().stream().map(languageCode -> {
                    String[] tokens = languageCode.split("-");
                    StringJoiner lang = new StringJoiner(" ");
                    if (tokens.length > 1) {
                        if (tokens[1].length() != 2) {
                            LOGGER.error(String.format("Country %s includes a language with country code '%s'",
                                    country.getName(),tokens[1]));
                            lang.add(tokens[1]);
                        } else {
                            if (!(tokens[1].equals(tokens[0].toUpperCase()))) {
                                Country langCountry = countryServiceAPI.getCountry(tokens[1]);
                                lang.add(langCountry.getName());
                            }
                        }
                    }
                    if (tokens[0].length() == 2) {
                        Language language = languageServiceAPI.getLanguageByIso3691(tokens[0]);
                        lang.add(language.getName());
                    } else if (tokens[0].length() == 3) {
                        Language language;
                        try {
                            language = languageServiceAPI.getLanguageByIso3692(tokens[0]);
                        } catch (ResponseStatusException e) {
                            language = languageServiceAPI.getLanguageByIso3693(tokens[0]);
                        }
                        lang.add(language.getName());
                    } else {
                        LOGGER.error(String.format("Country %s includes a language code '%s'",
                                country.getName(),tokens[0]));
                        lang.add(tokens[0]);
                    }
                    return lang.toString();
                }).toList();
                country.setLanguages(languageNames);

                String formattedCurrency;
                Double exchangeRate;

                if (country.getCurrencyCode().equals(DEFAULT_BASE_CURRENCY)) {
                    exchangeRate = 1.0;
                    formattedCurrency = String.format("%s (%s)",countryCurrency.getName(),countryCurrency.getCode());
                } else {
                    Currency baseCurrency = currencyServiceAPI.getCurrency(DEFAULT_BASE_CURRENCY);
                    exchangeRate = countryCurrency.getUsdRate();
                    if (countryCurrency.getSymbol().equals(countryCurrency.getCode()))
                        formattedCurrency = String.format("%s, Exchange Rate: %s %.2f to %s1.00",
                            countryCurrency.getName(),countryCurrency.getSymbol(),
                            countryCurrency.getUsdRate(),baseCurrency.getSymbol());
                    else
                        formattedCurrency = String.format("%s (%s), Exchange Rate: %s%.2f to %s1.00",
                            countryCurrency.getName(),countryCurrency.getCode(),countryCurrency.getSymbol(),
                            countryCurrency.getUsdRate(),baseCurrency.getSymbol());
                }
                minExchangeRateValue = Math.min(minExchangeRateValue,exchangeRate);
                maxExchangeRateValue = Math.max(maxExchangeRateValue,exchangeRate);
                LOGGER.info("minExchangeRateValue: " + minExchangeRateValue);
                LOGGER.info("maxExchangeRateValue: " + maxExchangeRateValue);
                country.setCurrencyCode(formattedCurrency);
                countryList.add(country);
                countryLocations.sort(Comparator.comparing(Location::getName));
            }

            countryList.sort(Comparator.comparing(Country::getName));

            for (Country country : countryList)
                countryLocationsList.add(locationListGroupedByCountryCode.get(country.getCode()));
            continentCountryList.add(countryList);
            continentCountryLocationsList.add(countryLocationsList);
        }
        model.addAttribute("minExchangeRateValue",Math.min(minExchangeRateValue,1.0));
        model.addAttribute("maxExchangeRateValue",Math.max(maxExchangeRateValue,0.0));
        model.addAttribute("continentList", Continent.values());
        model.addAttribute("continentCountryList", continentCountryList);
        model.addAttribute("continentCountryLocationsList", continentCountryLocationsList);
        model.addAttribute("locationFilter",new LocationFilter());
    }

    @GetMapping("/saved")
    public String getSaved(Model model) {
        addDefaultAttributes(model,new LocationFilter());
        return "fragments/tab :: saved-tab";
    }

    @GetMapping("/saved-locations")
    public String getSavedLocations(Model model,@ModelAttribute LocationFilter locationFilter) {
        addDefaultAttributes(model, locationFilter);
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
            if (deleted) {
                LOGGER.info(String.format("successfully deleted location with id: %d", locationId));
                tripsController.removeDeletedLocationFromRecents(locationId);
            }
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
