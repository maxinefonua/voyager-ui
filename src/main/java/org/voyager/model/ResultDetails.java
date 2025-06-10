package org.voyager.model;

import lombok.Builder;
import lombok.Data;
import org.voyager.model.airport.Airport;
import org.voyager.model.location.Location;
import org.voyager.model.result.ResultSearch;

import java.util.List;

@Builder @Data
public class ResultDetails {
    ResultSearch resultSearch;
    Location location;
    List<Airport> airportList;
}
