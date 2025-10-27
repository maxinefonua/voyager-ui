package org.voyager.commons.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor
public class AirportCodes {
    private List<String> codes = new ArrayList<>();
}
