package org.voyager.service.impl;

import org.voyager.sdk.model.AirlinePathQuery;
import org.voyager.commons.model.route.AirlinePath;
import org.voyager.commons.model.route.PathResponse;
import org.voyager.sdk.service.PathService;

import java.util.List;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class PathServiceAPI {
    private final PathService pathService;

    PathServiceAPI(PathService pathService) {
        this.pathService = pathService;
    }

    public PathResponse<AirlinePath> getPathAirlineList(AirlinePathQuery airlinePathQuery) {
        return unwrapEither(this.pathService.getAirlinePathResponse(airlinePathQuery));
    }
}
