package org.voyager.service.impl;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.error.HttpStatus;
import org.voyager.error.ServiceError;
import org.voyager.model.country.Continent;
import org.voyager.model.location.*;
import org.voyager.service.LocationService;

import java.util.List;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class LocationServiceAPI {
    private final LocationService locationService;
    private final Logger LOGGER = LoggerFactory.getLogger(LocationServiceAPI.class);

    LocationServiceAPI(LocationService locationService) {
        this.locationService = locationService;
    }

    public List<Location> getLocations(Status status) {
        return unwrapEither(locationService.getLocations(status));
    }

    public List<Location> getLocations(Source source, Continent continent, List<Status> statusList) {
        return unwrapEither(locationService.getLocations(source,continent,statusList));
    }

    public List<Location> getLocations(Integer limit) {
        return unwrapEither(locationService.getLocations(limit));
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

    public Boolean deleteLocation(Integer id) {
        return unwrapEither(locationService.deleteLocation(id));
    }
}
