package org.voyager.service.impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.config.VoyagerAPIConfig;
import org.voyager.model.AirportDisplay;
import org.voyager.model.TownDisplay;
import org.voyager.model.response.VoyagerListResponse;
import org.voyager.model.response.VoyagerResponseAPI;
import org.voyager.model.result.LookupAttribution;
import org.voyager.model.result.ResultSearch;
import org.voyager.service.VoyagerAPI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class VoyagerAPIService implements VoyagerAPI {
    @Autowired
    VoyagerAPIConfig voyagerAPIConfig;
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final Logger LOGGER = LoggerFactory.getLogger(VoyagerAPIService.class);

    public VoyagerListResponse<ResultSearch> lookup(String query, int skipRows) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String lookupURL = voyagerAPIConfig.buildLookupURL(encodedQuery,skipRows);
        LOGGER.info(String.format("full lookup URL: %s",lookupURL));
        ResponseEntity<VoyagerListResponse<ResultSearch>> searchResponse = restTemplate.exchange(
                lookupURL,
                HttpMethod.GET,
                voyagerAPIConfig.getHttpEntity(),
                new ParameterizedTypeReference<VoyagerListResponse<ResultSearch>>() {});
        // TODO: update error handling and message
        if (searchResponse.getStatusCode().value() != 200 || !searchResponse.hasBody()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error fetching search results. Status code: ");
            sb.append(searchResponse.getStatusCode().value());
            sb.append(" returned from endpoint: ");
            sb.append(lookupURL);
            if (searchResponse.hasBody()) {
                sb.append("\n");
                sb.append("Response: ");
                sb.append(searchResponse.getBody());
            }
            LOGGER.error(sb.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Error fetching search results for query '%s'",query));
        }
        return searchResponse.getBody();
    }

    @Override
    public LookupAttribution lookupAttribution() {
        String attributionURL = voyagerAPIConfig.buildLookupAttributionURL();
        LOGGER.info(String.format("Fetching lookup attribution from URL: %s",attributionURL));
        ResponseEntity<LookupAttribution> searchResponse = restTemplate.exchange(
                attributionURL,
                HttpMethod.GET,
                voyagerAPIConfig.getHttpEntity(),
                LookupAttribution.class);
        return searchResponse.getBody();
    }

    @Override
    public VoyagerResponseAPI<TownDisplay> towns() {
        String townsURL = voyagerAPIConfig.buildGetTownsURL();
        System.out.println("full towns URL: " + townsURL);
        ResponseEntity<List<TownDisplay>> townsResponse = restTemplate
                .exchange(townsURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        new ParameterizedTypeReference<List<TownDisplay>>() {});
        // TODO: null check
        List<TownDisplay> towns = townsResponse.getBody();
        assert towns != null;
        return new VoyagerResponseAPI<>(towns.size(),towns);
    }

    @Override
    public List<AirportDisplay> nearbyAirports(double latitude, double longitude, int limit) {
        String nearbyAirportsURL = voyagerAPIConfig.buildNearbyAirportsURL(latitude,longitude,limit);
        LOGGER.info("full nearbyAirports URL: " + nearbyAirportsURL);
        ResponseEntity<List<AirportDisplay>> airportsResponse = restTemplate
                .exchange(nearbyAirportsURL,
                        HttpMethod.GET,
                        voyagerAPIConfig.getHttpEntity(),
                        new ParameterizedTypeReference<List<AirportDisplay>>() {});
        if (airportsResponse.getStatusCode().value() != 200 || !airportsResponse.hasBody()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error fetching search results. Status code: ");
            sb.append(airportsResponse.getStatusCode().value());
            sb.append(" returned from endpoint: ");
            sb.append(nearbyAirportsURL);
            if (airportsResponse.hasBody()) {
                sb.append("\n");
                sb.append("Response: ");
                sb.append(airportsResponse.getBody());
            }
            LOGGER.error(sb.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching nearby airports");
        }
        return airportsResponse.getBody();
    }
}
