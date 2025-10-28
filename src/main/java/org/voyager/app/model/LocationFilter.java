package org.voyager.commons.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LocationFilter {
    Boolean includeArchived = false;
}
