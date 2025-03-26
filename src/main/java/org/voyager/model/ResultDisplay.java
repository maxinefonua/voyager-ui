package org.voyager.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.voyager.model.response.geonames.GeoName;

import java.util.List;

@AllArgsConstructor
@Getter
public class ResultDisplay {
    String name;
    String adminName1;
    String countryCode;
    String countryName;
    Float southBound;
    Float westBound;
    Float northBound;
    Float eastBound;
    Float longitude;
    Float latitude;
    String fclName;

    public static List<ResultDisplay> convertGeoNameToResultDisplayList(List<GeoName> geoNameList){
        return geoNameList.stream().map(geoName -> new ResultDisplay(
                geoName.getName(),
                geoName.getAdminName1(),
                geoName.getCountryCode(),
                geoName.getCountryName(),
                geoName.getBoundingBox().getSouth(),
                geoName.getBoundingBox().getWest(),
                geoName.getBoundingBox().getNorth(),
                geoName.getBoundingBox().getEast(),
                geoName.getLng(),
                geoName.getLat(),
                geoName.getFclName()
        )).toList();
    }
}
