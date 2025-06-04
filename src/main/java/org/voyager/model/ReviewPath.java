package org.voyager.model;

import lombok.*;
import org.voyager.model.route.Route;

import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor
@AllArgsConstructor
@ToString(includeFieldNames = false)
public class ReviewPath {
    @NonNull
    String startAirport;
    @NonNull
    String endAirport;
    Integer startLocationId;
    Integer endLocationId;
    List<Integer> routeIdList = new ArrayList<>();
}
