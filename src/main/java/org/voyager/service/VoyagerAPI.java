package org.voyager.service;

import org.voyager.model.ResultDisplay;
import org.voyager.model.TownDisplay;
import org.voyager.model.response.VoyagerResponseAPI;

public interface VoyagerAPI {
    public abstract VoyagerResponseAPI<ResultDisplay> lookup(String query, int skipRows);
    public abstract VoyagerResponseAPI<TownDisplay> towns();
}
