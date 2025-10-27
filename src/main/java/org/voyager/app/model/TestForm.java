package org.voyager.commons.model;

import lombok.*;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString(includeFieldNames = false)
public class TestForm {
    String filter;
    String airportCode;
}
