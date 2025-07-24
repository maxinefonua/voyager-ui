package org.voyager.service.impl;

import lombok.NonNull;
import org.voyager.model.country.Continent;
import org.voyager.model.country.Country;
import org.voyager.service.CountryService;
import java.util.List;
import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class CountryServiceAPI {
    private final CountryService countryService;

    CountryServiceAPI(@NonNull CountryService countryService) {
        this.countryService = countryService;
    }

    public List<Country> getCountries() {
        return unwrapEither(countryService.getCountries());
    }

    public List<Country> getCountries(List<Continent> continentList) {
        return unwrapEither(countryService.getCountries(continentList));
    }

    public Country getCountry(String countryCode) {
        return unwrapEither(countryService.getCountry(countryCode));
    }
}
