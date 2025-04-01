package org.voyager.service;

import org.voyager.model.AirportDisplay;
import org.voyager.model.ResultDisplay;
import org.voyager.model.TownDisplay;
import org.voyager.model.response.VoyagerListResponse;
import org.voyager.model.response.VoyagerResponseAPI;

import java.util.List;

public interface VoyagerAPI {
    public abstract VoyagerListResponse<ResultDisplay> lookup(String query, int skipRows);
    public abstract VoyagerResponseAPI<TownDisplay> towns();
    public abstract List<AirportDisplay> nearbyAirports(double latitude,double longitude,int limit);
}
