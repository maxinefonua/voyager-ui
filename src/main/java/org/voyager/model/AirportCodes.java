package org.voyager.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder @Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(includeFieldNames = false)
public class AirportCodes {
    private List<String> codes = new ArrayList<>();
}
