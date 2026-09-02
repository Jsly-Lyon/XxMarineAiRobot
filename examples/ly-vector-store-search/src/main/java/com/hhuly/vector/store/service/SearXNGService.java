package com.hhuly.vector.store.service;

import com.hhuly.vector.store.model.SearchResult;

import java.util.List;

public interface SearXNGService {

    /**
     * 调用 SearXNG Api, 获取搜索结果
     * @param query 搜索关键词
     * @return
     */
    List<SearchResult> search(String query);
}
