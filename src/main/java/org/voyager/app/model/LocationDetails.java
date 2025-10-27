package org.voyager.commons.model;

import lombok.Builder;
import lombok.Data;
import org.voyager.commons.model.airport.Airport;
import org.voyager.commons.model.location.Location;

import java.util.List;

@Builder @Data
public class LocationDetails {
    Location location;
    List<Airport> airportList;
}
