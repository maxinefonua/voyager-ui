package org.voyager.model;

import lombok.*;
import org.voyager.model.result.ResultSearch;

@Builder
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(includeFieldNames = false)
public class LocationForm {
    ResultSearch resultSearch;
    String airportCode;
}
