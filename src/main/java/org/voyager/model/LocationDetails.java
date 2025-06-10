package org.voyager.model;

import lombok.Builder;
import lombok.Data;
import org.voyager.model.airport.Airport;
import org.voyager.model.location.Location;

import java.util.List;

@Builder @Data
public class LocationDetails {
    Location location;
    List<Airport> airportList;
}
