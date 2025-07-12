package org.voyager.utils;

import lombok.NonNull;
import org.voyager.model.location.LocationForm;
import org.voyager.model.result.ResultSearch;
import org.voyager.model.result.ResultSearchFull;

public class LocationMapperUtils {
    public static LocationForm toLocationForm(@NonNull ResultSearchFull resultSearchFull) {
        ResultSearch resultSearch = resultSearchFull.getResultSearch();
        return LocationForm.builder()
                .name(resultSearch.getName())
                .subdivision(resultSearch.getSubdivision())
                .countryCode(resultSearch.getCountryCode())
                .latitude(resultSearch.getLatitude())
                .longitude(resultSearch.getLongitude())
                .west(resultSearchFull.getBbox()[0])
                .south(resultSearchFull.getBbox()[1])
                .east(resultSearchFull.getBbox()[2])
                .north(resultSearchFull.getBbox()[3])
                .source(resultSearch.getSource().name())
                .sourceId(resultSearch.getSourceId())
                .build();
    }
}
