package org.voyager.model;

import lombok.*;
import org.voyager.model.result.ResultSearch;

@Builder
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(includeFieldNames = false)
public class LocationForm {
    String name;
    String subdivision;
    String countryCode;
    Double latitude;
    Double longitude;
    String airportCode;
    Double west;
    Double south;
    Double east;
    Double north;

}
