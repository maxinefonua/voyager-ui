package org.voyager.app.service.impl;

import org.voyager.sdk.model.SearchQuery;
import org.voyager.commons.model.location.Source;
import org.voyager.commons.model.response.SearchResult;
import org.voyager.commons.model.result.LookupAttribution;
import org.voyager.commons.model.result.ResultSearch;
import org.voyager.commons.model.result.ResultSearchFull;
import org.voyager.sdk.service.SearchService;


import static org.voyager.app.service.impl.VoyagerServiceImpl.unwrapEither;

public class SearchServiceAPI {
    private final SearchService searchService;
    SearchServiceAPI(SearchService searchService) {
        this.searchService = searchService;
    }

    public SearchResult<ResultSearch> search(SearchQuery searchQuery) {
        return unwrapEither(searchService.search(searchQuery));
    }

    public ResultSearchFull fetchResultSearchFull(String sourceId) {
        return unwrapEither(searchService.fetchResultSearchFull(sourceId));
    }

    public LookupAttribution getLookupAttribution() {
        return unwrapEither(searchService.attribution());
    }

    public Source getSource() {
        return Source.valueOf(unwrapEither(searchService.attribution()).getName().toUpperCase());
    }
}
