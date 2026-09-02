package com.hhuly.vector.store.service;

import com.hhuly.vector.store.model.SearchResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface SearchResultContentFetcherService {


    /**
     * 并发批量获取搜索结果页面的内容
     *
     * @param searchResults
     * @param timeout
     * @param unit
     * @return
     */
    CompletableFuture<List<SearchResult>> batchFetch(List<SearchResult> searchResults,
                                                     long timeout,
                                                     TimeUnit unit);
}

