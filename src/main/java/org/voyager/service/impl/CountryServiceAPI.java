package org.voyager.service.impl;

import lombok.NonNull;
import org.voyager.model.country.Country;
import org.voyager.service.CountryService;

import java.util.List;
import java.util.Optional;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class CountryServiceAPI {
    private final CountryService countryService;
    private List<Country> allCountries;

    CountryServiceAPI(@NonNull CountryService countryService) {
        this.countryService = countryService;
    }

    public Country getCountry(String countryCode) {
        if (allCountries == null) allCountries = unwrapEither(countryService.getCountries());
        Optional<Country> exists = allCountries.stream().filter(country ->
                country.getCode().equals(countryCode)).findAny();
        if (exists.isPresent()) return exists.get();
        Country country = unwrapEither(countryService.getCountry(countryCode));
        allCountries.add(country);
        return country;
    }
}
