package org.voyager.commons.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Builder @Getter
public class FlightDetails {
    String flightNumber;
    @Setter
    String departureTimeFormatted;
    String arrivalTimeFormatted;
    String durationFormatted;
}
