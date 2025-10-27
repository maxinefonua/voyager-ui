package org.voyager.service.impl;

import org.voyager.commons.model.location.*;
import org.voyager.sdk.service.LocationService;
import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class LocationServiceAPI {
    private final LocationService locationService;

    LocationServiceAPI(LocationService locationService) {
        this.locationService = locationService;
    }

    public Location getLocation(Integer id) {
        return unwrapEither(locationService.getLocation(id));
    }

    public Location patchLocation(Integer id, LocationPatch locationPatch) {
        return unwrapEither(locationService.patchLocation(id,locationPatch));
    }

    public Location addLocation(LocationForm locationForm) {
        return unwrapEither(locationService.createLocation(locationForm));
    }

    public Location getLocation(Source source, String sourceId) {
        return unwrapEither(locationService.getLocation(source, sourceId));
    }
}
