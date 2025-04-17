package org.voyager.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.voyager.model.result.ResultSearch;

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
    @Size(min=3,max=3,message = "Airport must be a valid 3-letter IATA code")
    String airportCode;
    Double west;
    Double south;
    Double east;
    Double north;

}
