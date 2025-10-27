package org.voyager.commons.model;

import lombok.Builder;
import lombok.Data;
import org.voyager.commons.model.airport.Airport;
import org.voyager.commons.model.location.Location;
import org.voyager.commons.model.result.ResultSearch;

import java.util.List;

@Builder @Data
public class ResultDetails {
    ResultSearch resultSearch;
    Location location;
    List<Airport> airportList;
}
