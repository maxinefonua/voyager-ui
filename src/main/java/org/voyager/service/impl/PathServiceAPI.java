package org.voyager.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.voyager.model.Airline;
import org.voyager.model.route.PathAirline;
import org.voyager.model.route.PathResponse;
import org.voyager.service.PathService;

import java.util.List;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class PathServiceAPI {
    private final PathService pathService;

    PathServiceAPI(PathService pathService) {
        this.pathService = pathService;
    }

    public PathResponse<PathAirline> getPathAirlineList(List<String> originList, List<String> destinationList,
                                                        List<String> excludeAirportList, List<Integer> excludeRouteIdList) {
        return unwrapEither(this.pathService.getPathAirlineList(originList,destinationList,excludeAirportList,excludeRouteIdList));
    }

    public PathResponse<PathAirline> getPathAirlineList(List<String> originList, List<String> destinationList,
                                                        List<String> excludeAirportList, List<Integer> excludeRouteIdList,
                                                        Airline airline) {
        return unwrapEither(this.pathService.getPathAirlineList(originList,destinationList,excludeAirportList,excludeRouteIdList,airline));
    }
}
