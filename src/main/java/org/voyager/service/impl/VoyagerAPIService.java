package org.voyager.service.impl;

import io.vavr.Tuple2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.voyager.config.VoyagerAPIConfig;
import org.voyager.model.ResultDisplay;
import org.voyager.model.TownDisplay;
import org.voyager.model.response.SearchResponseGeoNames;
import org.voyager.model.response.VoyagerResponseAPI;
import org.voyager.service.VoyagerAPI;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
public class VoyagerAPIService implements VoyagerAPI {
    @Autowired
    VoyagerAPIConfig voyagerAPIConfig;
    RestTemplate restTemplate = new RestTemplate();

    public VoyagerResponseAPI<ResultDisplay> lookup(String query, int skipRows) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String lookupURL = voyagerAPIConfig.buildLookupURL(encodedQuery,skipRows);
            System.out.println("full lookup URL: " + lookupURL);
            ResponseEntity<SearchResponseGeoNames> searchResponse = restTemplate
                    .exchange(lookupURL,
                            HttpMethod.GET,
                            voyagerAPIConfig.getHttpEntity(),
                            SearchResponseGeoNames.class);
            SearchResponseGeoNames responseBody = searchResponse.getBody();
            // TODO: null check
            return new VoyagerResponseAPI<>(responseBody.getTotalResultsCount(),
                    ResultDisplay.convertGeoNameToResultDisplayList(responseBody.getGeoNames()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public VoyagerResponseAPI<TownDisplay> towns() {
        String townsURL = voyagerAPIConfig.buildGetTownsURL();
        System.out.println("full towns URL: " + townsURL);
        ResponseEntity<TownDisplay[]> townsResponse = restTemplate
                .exchange(townsURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        TownDisplay[].class);
        // TODO: null check
        TownDisplay[] towns = townsResponse.getBody();
        return new VoyagerResponseAPI<>(towns.length, Arrays.asList(towns));
    }
}
