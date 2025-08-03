package org.voyager.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Builder @Data @ToString
public class Option {
    Double longitude;
    Double latitude;
    String display;
    String name;
    String city;
    String subdivision;
    String country;
    String value;
    String elementName;
    @Builder.Default
    Boolean selected = false;
    @Builder.Default
    Boolean disabled = false;

    public static List<Option> getFilterOptions(TripFilter tripFilter) {
        switch (tripFilter) {
            case AIRPORT -> {
                return airportFilters();
            }
            case LOCATION -> {
                return locationFilters();
            }
            default -> {
                return new ArrayList<>();
            }
        }
    }

    private static List<Option> airportFilters() {
        List<Option> airportFilters = new ArrayList<>();
        airportFilters.add(Option.builder().display("Delta Airports").value("DELTA").build());
        airportFilters.add(Option.builder().display("All Civil").value("CIVIL").build());
        airportFilters.add(Option.builder().display("Military").value("MILITARY").build());
        return airportFilters;
    }

    private static List<Option> locationFilters() {
        List<Option> locationFilters = new ArrayList<>();
        locationFilters.add(Option.builder().display("Saved Locations").value("SAVED").build());
        locationFilters.add(Option.builder().display("Archived").value("ARCHIVED").build());
        return locationFilters;
    }
}
