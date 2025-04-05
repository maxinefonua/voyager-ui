package org.voyager.service;

import org.voyager.model.AirportDisplay;
import org.voyager.model.TownDisplay;
import org.voyager.model.response.VoyagerListResponse;
import org.voyager.model.response.VoyagerResponseAPI;
import org.voyager.model.result.LookupAttribution;
import org.voyager.model.result.ResultSearch;

import java.util.List;

public interface VoyagerAPI {
    public abstract VoyagerListResponse<ResultSearch> lookup(String query, int skipRows);
    public abstract LookupAttribution lookupAttribution();
    public abstract VoyagerResponseAPI<TownDisplay> towns();
    public abstract List<AirportDisplay> nearbyAirports(double latitude,double longitude,int limit);
}
