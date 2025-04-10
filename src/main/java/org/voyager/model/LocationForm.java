package org.voyager.model;

import lombok.*;

@Builder
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(includeFieldNames = false)
public class LocationForm {
    String name;
    String airportCode;
}
