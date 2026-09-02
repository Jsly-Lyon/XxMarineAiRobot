package com.hhuly.ai.robot.service;

import com.hhuly.ai.robot.model.dto.search.SearchResultDTO;

import java.util.List;

public interface SearXNGService {

    /**
     * 调用 SearXNG Api, 获取搜索结果
     * @param query 搜索关键词
     * @return
     */
    List<SearchResultDTO> search(String query);
}
