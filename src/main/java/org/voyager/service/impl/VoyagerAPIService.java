package org.voyager.service.impl;

import io.vavr.Tuple2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.voyager.config.VoyagerAPIConfig;
import org.voyager.model.ResultDisplay;
import org.voyager.model.TownDisplay;
import org.voyager.model.response.SearchResponseGeoNames;
import org.voyager.model.response.VoyagerResponseAPI;
import org.voyager.service.VoyagerAPI;
import java.util.List;

@Service
public class VoyagerAPIService implements VoyagerAPI {
    @Autowired
    VoyagerAPIConfig voyagerAPIConfig;

    RestTemplate restTemplate = new RestTemplate();

    public VoyagerResponseAPI<ResultDisplay> lookup(String query, int skipRows) {
        String lookupURL = voyagerAPIConfig.buildLookupURL(query,skipRows);
        System.out.println("full lookup URL: " + lookupURL);
        ResponseEntity<SearchResponseGeoNames> searchResponse = restTemplate.getForEntity(lookupURL,SearchResponseGeoNames.class);
        SearchResponseGeoNames responseBody = searchResponse.getBody();
        // TODO: null check
        return new VoyagerResponseAPI<>(responseBody.getTotalResultsCount(),ResultDisplay.convertGeoNameToResultDisplayList(responseBody.getGeoNames()));
    }

    @Override
    public Tuple2<Integer, List<TownDisplay>> getTowns(String query, int skipRows) {
        return new Tuple2<>(0,List.of());
    }
}
