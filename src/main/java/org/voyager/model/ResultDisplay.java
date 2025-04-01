package org.voyager.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.voyager.utils.MapperUtils;

@Builder @Getter
 @ToString(includeFieldNames = false)
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

    private static final MapperUtils<ResultDisplay> mapper = new MapperUtils<>(ResultDisplay.class);

    public String toJson() {
        return mapper.mapToJson(this);
    }

    public static ResultDisplay fromJson(String jsonString) {
        return mapper.mapFromJson(jsonString);
    }
}
