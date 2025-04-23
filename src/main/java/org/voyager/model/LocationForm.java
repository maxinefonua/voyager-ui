package org.voyager.model;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Setter;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(includeFieldNames = false)
public class LocationForm {
    Integer index;
    String name;
    String subdivision;
    String countryCode;
    Double latitude;
    Double longitude;
    @Pattern(regexp = "^[a-zA-Z]{3}$",
            message = "Airport must be a valid 3-letter IATA code")
    String airportCode;
    Double west;
    Double south;
    Double east;
    Double north;

}
