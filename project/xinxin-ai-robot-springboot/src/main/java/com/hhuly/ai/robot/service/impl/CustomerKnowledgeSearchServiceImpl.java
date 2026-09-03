package com.hhuly.ai.robot.service.impl;

import com.hhuly.ai.robot.constant.CustomerDocMetadata;
import com.hhuly.ai.robot.constant.MarineSynonymGroups;
import com.hhuly.ai.robot.domain.dos.DocChunkDO;
import com.hhuly.ai.robot.domain.mapper.DocChunkMapper;
import com.hhuly.ai.robot.service.CustomerKnowledgeSearchService;
import com.hhuly.ai.robot.utils.ChineseTokenizer;
import com.hhuly.ai.robot.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 客服知识检索：Dense(Milvus/BGE-M3) + 关键词(PG tsvector/jieba) 双路 -> RRF。
 * 增强：84 组海洋同义词 OR 展开、同义改写多查询（伪 MultiQuery）、跨变体二次 RRF、
 * 可选 Jina Rerank（失败自动降级）。
 **/
@Slf4j
@Service
public class CustomerKnowledgeSearchServiceImpl implements CustomerKnowledgeSearchService {

    /** RRF 常量（经典取 60） */
    private static final int RRF_K = 60;

    /** 单路单次召回 */
    private static final int ROUTE_TOP_K = 8;

    /** 多查询变体上限（含原始 query） */
    private static final int MAX_VARIANTS = 3;

    /** 关键词路停用词（中英功能词，避免 OR 召回过宽污染 RRF） */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "是", "什么", "怎么", "如何", "为什么", "哪些", "一个", "在", "了", "用", "让", "把", "与", "和", "及", "或",
            "可以", "这篇", "本文", "里面", "干嘛", "吗", "呢",
            "which", "what", "how", "why", "when", "where", "who",
            "is", "are", "was", "were", "be", "been", "being", "does", "do", "did",
            "the", "a", "an", "of", "in", "on", "at", "to", "for", "and", "or", "not",
            "it", "its", "this", "that", "these", "those", "with", "from", "by", "as", "used", "use");

    private final VectorStore vectorStore;
    private final DocChunkMapper docChunkMapper;

    @Resource
    private OkHttpClient okHttpClient;

    @Value("${customer-service.rerank.enabled:false}")
    private boolean rerankEnabled;
    @Value("${customer-service.rerank.base-url:https://api.jina.ai/v1/rerank}")
    private String rerankBaseUrl;
    @Value("${customer-service.rerank.api-key:}")
    private String rerankApiKey;
    @Value("${customer-service.rerank.model:jina-reranker-v2-base-multilingual}")
    private String rerankModel;

    public CustomerKnowledgeSearchServiceImpl(VectorStore vectorStore, DocChunkMapper docChunkMapper) {
        this.vectorStore = vectorStore;
        this.docChunkMapper = docChunkMapper;
    }

    @Override
    public List<Document> search(String query, int topK, Long userId) {
        List<String> variants = queryVariants(query);   // 原始 + 同义改写变体（多查询）

        List<List<String>> allRankings = new ArrayList<>();
        Map<String, Document> byDocId = new HashMap<>();

        for (String variant : variants) {
            // 1) Dense 路
            List<Document> denseDocs = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(variant)
                    .topK(ROUTE_TOP_K)
                    .filterExpression(buildOwnerFilter(userId))
                    .build());
            List<String> denseIds = new ArrayList<>();
            for (Document doc : denseDocs) {
                byDocId.putIfAbsent(doc.getId(), doc);
                denseIds.add(doc.getId());
            }
            allRankings.add(denseIds);

            // 2) Keyword 路（jieba + 同义词 OR 展开）
            String tsQuery = keywordTsQuery(variant);
            if (tsQuery != null) {
                try {
                    List<String> kwIds = new ArrayList<>();
                    for (DocChunkDO chunk : docChunkMapper.searchTopK(tsQuery, userId, ROUTE_TOP_K)) {
                        byDocId.putIfAbsent(chunk.getDocId(), new Document(chunk.getDocId(), chunk.getContent(),
                                Map.of(CustomerDocMetadata.KEY_OWNER_USER_ID, chunk.getOwner())));
                        kwIds.add(chunk.getDocId());
                    }
                    allRankings.add(kwIds);
                } catch (Exception e) {
                    // 关键词路失败自动降级为仅 Dense
                    log.warn("## 关键词检索失败，该变体仅用 Dense 路。query={}", variant, e);
                }
            }
        }

        // 3) 跨变体二次 RRF：把「每个变体各路的排名」汇成一个大排名集再融合
        List<String> mergedIds = rrfMerge(allRankings);

        // 4) 输出 topK
        List<Document> result = new ArrayList<>();
        for (String id : mergedIds) {
            Document doc = byDocId.get(id);
            if (doc != null) {
                result.add(doc);
                if (result.size() >= topK) {
                    break;
                }
            }
        }

        // 5) 可选 Jina Rerank（失败自动降级为当前结果）
        if (rerankEnabled && hasText(rerankApiKey)) {
            return rerank(query, result, topK);
        }
        return truncate(result, topK);
    }

    /**
     * 多查询分解：原始 query + 把命中间义 token 逐次替换为组内其它写法生成变体（≤MAX_VARIANTS）
     */
    private List<String> queryVariants(String query) {
        List<String> tokens = keepTokens(ChineseTokenizer.tokenize(query));
        if (tokens.isEmpty()) {
            return List.of(query);
        }
        Set<String> variants = new LinkedHashSet<>();
        variants.add(query);
        for (String token : tokens) {
            if (variants.size() >= MAX_VARIANTS) {
                break;
            }
            List<String> group = MarineSynonymGroups.groupOf(token);
            if (group == null) {
                continue;
            }
            // 用组内另一个写法替换本 token，生成改写变体
            for (String member : group) {
                if (member.equalsIgnoreCase(token)) {
                    continue;
                }
                String variant = query.replace(token, member);
                if (!variants.contains(variant)) {
                    variants.add(variant);
                    break; // 每个命中词只产一个变体
                }
            }
        }
        return new ArrayList<>(variants);
    }

    /**
     * 关键词 tsquery：token 命中同义词组则展开为 OR 链，组间用 AND
     */
    private String keywordTsQuery(String query) {
        List<String> tokens = keepTokens(ChineseTokenizer.tokenize(query));
        if (tokens.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (String token : tokens) {
            List<String> group = MarineSynonymGroups.groupOf(token);
            if (group == null) {
                String term = toTsTerm(token);
                if (term != null) {
                    parts.add(term);
                }
            } else {
                // 含空格的英文别名（如 algal bloom）需转成短语运算符：algal <-> bloom
                List<String> orTerms = new ArrayList<>();
                for (String member : group) {
                    String term = toTsTerm(member);
                    if (term != null) {
                        orTerms.add(term);
                    }
                }
                if (!orTerms.isEmpty()) {
                    parts.add("(" + String.join(" | ", orTerms) + ")");
                }
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        // 用 OR 连接：命中任一关键词即召回（按 ts_rank 命中词数排序），
        // 避免整句 AND 在自然语言问句下几乎必然为空
        return String.join(" | ", parts);
    }

    /**
     * token 转 tsquery 词：过滤会破坏语法的保留字符；含空格（英文短语）时用短语运算符 <-> 连接
     */
    private static String toTsTerm(String word) {
        // 先去掉会破坏 tsquery 语法的保留字符，再按空白切词，最后用短语运算符连接
        String cleaned = word.trim().replaceAll("[&|!()<>:'*?\\-]", " ");
        String[] words = cleaned.split("\\s+");
        List<String> kept = new ArrayList<>();
        for (String w : words) {
            if (!w.isBlank()) {
                kept.add(w);
            }
        }
        return kept.isEmpty() ? null : String.join(" <-> ", kept);
    }

    /** 过滤停用词，只保留有检索意义的实义词 */
    private static List<String> keepTokens(List<String> tokens) {
        List<String> kept = new ArrayList<>();
        for (String token : tokens) {
            if (token != null && !token.isBlank() && !STOP_WORDS.contains(token.toLowerCase())) {
                kept.add(token);
            }
        }
        return kept;
    }

    /**
     * 数据隔离过滤：owner = 系统内置 或 当前用户
     */
    private String buildOwnerFilter(Long userId) {
        String owner = CustomerDocMetadata.KEY_OWNER_USER_ID;
        long systemOwner = CustomerDocMetadata.SYSTEM_OWNER_USER_ID;
        if (userId == null) {
            return String.format("%s == %d", owner, systemOwner);
        }
        return String.format("(%s == %d) or (%s == %d)", owner, systemOwner, owner, userId);
    }

    /**
     * 经典 RRF 融合：score(d) = Σ 1 / (K + rank(d))
     */
    private List<String> rrfMerge(List<List<String>> rankedLists) {
        Map<String, Double> score = new HashMap<>();
        for (List<String> list : rankedLists) {
            if (CollectionUtils.isEmpty(list)) {
                continue;
            }
            for (int i = 0; i < list.size(); i++) {
                score.merge(list.get(i), 1.0 / (RRF_K + i + 1), Double::sum);
            }
        }
        List<Map.Entry<String, Double>> entries = new ArrayList<>(score.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<String> merged = new ArrayList<>(entries.size());
        for (Map.Entry<String, Double> entry : entries) {
            merged.add(entry.getKey());
        }
        return merged;
    }

    /**
     * Jina Rerank：按相关性重排（失败自动降级为原结果截断）
     */
    private List<Document> rerank(String query, List<Document> docs, int topK) {
        if (docs.size() <= 1) {
            return docs;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", rerankModel);
            payload.put("query", query);
            payload.put("top_n", Math.max(topK, 1));
            payload.put("documents", docs.stream().map(Document::getText).toList());

            Request request = new Request.Builder()
                    .url(rerankBaseUrl)
                    .addHeader("Authorization", "Bearer " + rerankApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(JsonUtil.toJsonString(payload),
                            okhttp3.MediaType.get("application/json; charset=utf-8")))
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful() || !hasText(body)) {
                    log.warn("## Jina Rerank 调用失败，降级。code={}", response.code());
                    return truncate(docs, topK);
                }
                JsonNode root = JsonUtil.parseObject(body, JsonNode.class);
                JsonNode results = root != null ? root.get("results") : null;
                if (results == null || !results.isArray()) {
                    return truncate(docs, topK);
                }
                // 按 relevance_score 降序取 index
                List<int[]> scored = new ArrayList<>();
                for (JsonNode item : results) {
                    scored.add(new int[]{item.get("index").asInt(), (int) (item.get("relevance_score").asDouble() * 10000)});
                }
                scored.sort((a, b) -> Integer.compare(b[1], a[1]));

                List<Document> ordered = new ArrayList<>();
                for (int[] s : scored) {
                    int idx = s[0];
                    if (idx >= 0 && idx < docs.size()) {
                        ordered.add(docs.get(idx));
                    }
                    if (ordered.size() >= topK) {
                        break;
                    }
                }
                return ordered.isEmpty() ? truncate(docs, topK) : ordered;
            }
        } catch (Exception e) {
            log.warn("## Jina Rerank 异常，降级为原结果", e);
            return truncate(docs, topK);
        }
    }

    private List<Document> truncate(List<Document> docs, int topK) {
        return docs.size() <= topK ? docs : new ArrayList<>(docs.subList(0, topK));
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
