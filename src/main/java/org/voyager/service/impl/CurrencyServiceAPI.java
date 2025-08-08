package org.voyager.service.impl;

import org.voyager.model.currency.Currency;
import org.voyager.service.CurrencyService;

import java.util.List;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class CurrencyServiceAPI {
    private final CurrencyService currencyService;

    CurrencyServiceAPI(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    public List<Currency> getCurrencies() {
        return unwrapEither(currencyService.getCurrencies());
    }

    public List<Currency> getCurrencies(Boolean isActive) {
        return unwrapEither(currencyService.getCurrencies(isActive));
    }

    public Currency getCurrency(String code) {
        return unwrapEither(currencyService.getCurrency(code));
    }
}
