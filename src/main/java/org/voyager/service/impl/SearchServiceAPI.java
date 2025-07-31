package org.voyager.service.impl;

import io.vavr.control.Either;
import org.voyager.error.ServiceError;
import org.voyager.model.response.SearchResult;
import org.voyager.model.result.ResultSearch;
import org.voyager.model.result.ResultSearchFull;
import org.voyager.service.SearchService;

import static org.voyager.service.impl.VoyagerServiceImpl.unwrapEither;

public class SearchServiceAPI {
    private final SearchService searchService;
    public SearchServiceAPI(SearchService searchService) {
        this.searchService = searchService;
    }

    public SearchResult<ResultSearch> search(String query,int limit) {
        return unwrapEither(searchService.search(query,limit));
    }

    public ResultSearchFull fetchResultSearchFull(String sourceId) {
        return unwrapEither(searchService.fetchResultSearchFull(sourceId));
    }
}
