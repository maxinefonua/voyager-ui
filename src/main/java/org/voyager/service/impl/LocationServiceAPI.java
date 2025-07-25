package org.voyager.service.impl;

import org.voyager.model.country.Continent;
import org.voyager.model.location.Location;
import org.voyager.model.location.Source;
import org.voyager.model.location.Status;
import org.voyager.service.LocationService;

import java.util.List;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class LocationServiceAPI {
    private final LocationService locationService;

    public LocationServiceAPI(LocationService locationService) {
        this.locationService = locationService;
    }

    public List<Location> getLocations(Source source, Continent continent) {
        return unwrapEither(locationService.getLocations(source,continent));
    }

    public List<Location> getLocations(Source source, Continent continent, List<Status> statusList) {
        return unwrapEither(locationService.getLocations(source,continent,statusList));
    }
}
