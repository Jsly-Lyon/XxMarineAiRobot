package com.hhuly.vector.store.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量检索接口：将查询文本向量化后，在 Milvus 中执行相似度检索
 */
@RestController
@RequestMapping("/vector")
public class VectorSearchController {

    private static final String COLLECTION_NAME = "refrigerator_manual";
    private static final String VECTOR_FIELD = "embedding";
    private static final String CONTENT_FIELD = "content";
    private static final String DYNAMIC_META = "$meta";

    private static final Gson gson = new Gson();

    private final EmbeddingModel embeddingModel;
    private final MilvusClientV2 milvusClient;

    public VectorSearchController(EmbeddingModel embeddingModel, MilvusClientV2 milvusClient) {
        this.embeddingModel = embeddingModel;
        this.milvusClient = milvusClient;
    }

    /**
     * GET /api/search?query=冰箱怎么除霜&topK=3
     */
    @GetMapping(value = "/search")
    public SearchResponse search(@RequestParam(value = "query", required = false) String query,
                                 @RequestParam(value = "topK", defaultValue = "3") int topK) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query 不能为空");
        }

        // 1. 加载 Collection（幂等，已加载时无额外开销）
        milvusClient.loadCollection(LoadCollectionReq.builder().collectionName(COLLECTION_NAME).build());

        // 2. 向量化查询文本
        float[] vector = embeddingModel.embed(query);

        // 3. 构造检索请求，输出文档内容与动态元数据字段
        SearchResp resp = milvusClient.search(SearchReq.builder()
                .collectionName(COLLECTION_NAME)
                .annsField(VECTOR_FIELD)
                .metricType(IndexParam.MetricType.COSINE)
                .topK(topK)
                .outputFields(List.of(CONTENT_FIELD, DYNAMIC_META))
                .data(List.of(new FloatVec(vector)))
                .build());

        // 4. 解析结果（单查询，取第一组）
        List<List<SearchResp.SearchResult>> resultGroups = resp.getSearchResults();
        List<ResultItem> items = (resultGroups == null || resultGroups.isEmpty())
                ? Collections.emptyList()
                : resultGroups.get(0).stream().map(this::toResultItem).toList();

        return new SearchResponse(query, items);
    }

    /** 单条检索结果：文档内容 + 相似度 + 元数据 */
    private ResultItem toResultItem(SearchResp.SearchResult result) {
        Map<String, Object> entity = result.getEntity();

        Object contentValue = entity.get(CONTENT_FIELD);
        String content = contentValue != null ? contentValue.toString() : "";

        float score = result.getScore() != null ? result.getScore() : 0f;

        return new ResultItem(content, score, extractMetadata(entity));
    }

    /** 从查询结果中提取动态字段（章节/版本/紧急程度等中文元数据） */
    private Map<String, Object> extractMetadata(Map<String, Object> entity) {
        Object meta = entity.get(DYNAMIC_META);
        if (meta instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        if (meta instanceof JsonObject jsonObject) {
            return gson.fromJson(jsonObject, new TypeToken<Map<String, Object>>() {}.getType());
        }
        return Collections.emptyMap();
    }

    /** 检索响应 */
    public record SearchResponse(String query, List<ResultItem> results) {
    }

    /** 单条检索结果项 */
    public record ResultItem(String content, double score, Map<String, Object> metadata) {
    }
}
