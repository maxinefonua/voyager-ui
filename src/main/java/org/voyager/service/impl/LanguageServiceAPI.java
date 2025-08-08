package org.voyager.service.impl;

import org.voyager.model.language.Language;
import org.voyager.service.LanguageService;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class LanguageServiceAPI {
    private final LanguageService languageService;

    LanguageServiceAPI(LanguageService languageService) {
        this.languageService = languageService;
    }

    public Language getLanguageByIso3691(String iso3691) {
        return unwrapEither(languageService.getLanguageByIso6391(iso3691));
    }

    public Language getLanguageByIso3692(String iso3692) {
        return unwrapEither(languageService.getLanguageByIso6392(iso3692));
    }

    public Language getLanguageByIso3693(String iso3693) {
        return unwrapEither(languageService.getLanguageByIso6393(iso3693));
    }
}
