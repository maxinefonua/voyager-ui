package org.voyager.commons.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PathExclusions {
    boolean hasExclusions;
    List<String> airports;
    List<Integer> routeIds;

    public boolean getHasExclusions() {
        if (airports == null || routeIds == null) return false;
        return !airports.isEmpty() || !routeIds.isEmpty();
    }

    public PathExclusions() {
        airports = new ArrayList<>();
        routeIds = new ArrayList<>();
        hasExclusions = false;
    }
}
