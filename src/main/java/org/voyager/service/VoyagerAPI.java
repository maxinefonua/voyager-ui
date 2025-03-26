package org.voyager.service;

import io.vavr.Tuple2;
import org.voyager.model.ResultDisplay;
import org.voyager.model.TownDisplay;
import org.voyager.model.response.VoyagerResponseAPI;

import java.util.List;

public interface VoyagerAPI {
    public abstract VoyagerResponseAPI<ResultDisplay> lookup(String query, int skipRows);
    public abstract Tuple2<Integer,List<TownDisplay>> getTowns(String query, int skipRows);
}
