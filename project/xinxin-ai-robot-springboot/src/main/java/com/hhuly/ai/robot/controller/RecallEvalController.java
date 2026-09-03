package com.hhuly.ai.robot.controller;

import com.hhuly.ai.robot.constant.CustomerDocMetadata;
import com.hhuly.ai.robot.service.CustomerKnowledgeSearchService;
import com.hhuly.ai.robot.utils.JsonUtil;
import com.hhuly.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 召回率评测（开发用）：对比「单 Dense」与「双路 Dense+BM25 RRF」的 recall@topK
 *
 * @author: li
 * @date: 2026/9/3
 * @description: 评测集在 resources/cs-recall-queries.json；召回对象 = 系统内置文档(owner=0)，
 * 与登录用户无关（便于公平对照）。
 **/
@RestController
@RequestMapping("/customer-service/eval")
@Slf4j
public class RecallEvalController {

    @Resource
    private VectorStore vectorStore;
    @Resource
    private CustomerKnowledgeSearchService searchService;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvalSet {
        private Integer topK;
        private List<EvalQuery> queries;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvalQuery {
        private String query;
        private List<String> expect;
    }

    /**
     * 执行召回评测，返回逐条 + 汇总（含提升）
     */
    @PostMapping("/recall")
    public Response<String> recall() {
        try {
            EvalSet evalSet = readEvalSet();
            int topK = evalSet.getTopK() != null ? evalSet.getTopK() : 5;
            List<EvalQuery> queries = evalSet.getQueries();
            if (queries == null || queries.isEmpty()) {
                return Response.fail("评测集为空：resources/cs-recall-queries.json");
            }

            List<String> lines = new ArrayList<>();
            int denseHit = 0;
            int hybridHit = 0;

            for (EvalQuery q : queries) {
                // A：单 Dense（仅系统内置文档，owner=0）
                List<Document> denseDocs = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(q.getQuery())
                        .topK(topK)
                        .filterExpression(ownerFilter())
                        .build());
                // B：双路 Dense + BM25 -> RRF（userId=null -> 同样仅内置）
                List<Document> hybridDocs = searchService.search(q.getQuery(), topK, null);

                boolean d = hitAny(denseDocs, q.getExpect());
                boolean h = hitAny(hybridDocs, q.getExpect());
                if (d) denseHit++;
                if (h) hybridHit++;
                lines.add(String.format("Q: %s\n   Dense命中=%s | 双路RRF命中=%s", q.getQuery(), d, h));
            }

            double denseRate = 100.0 * denseHit / queries.size();
            double hybridRate = 100.0 * hybridHit / queries.size();
            lines.add("\n==== 汇总 (topK=" + topK + ", 共 " + queries.size() + " 条) ====");
            lines.add(String.format("单 Dense   recall@%d = %d/%d = %.1f%%", topK, denseHit, queries.size(), denseRate));
            lines.add(String.format("双路(Dense+BM25 RRF) recall@%d = %d/%d = %.1f%%", topK, hybridHit, queries.size(), hybridRate));
            lines.add(String.format("双路提升: %.1f 个百分点", hybridRate - denseRate));

            return Response.success(String.join("\n", lines));
        } catch (Exception e) {
            log.error("## 召回评测失败", e);
            return Response.fail("召回评测失败: " + e.getMessage());
        }
    }

    private EvalSet readEvalSet() throws Exception {
        try (InputStream in = new ClassPathResource("cs-recall-queries.json").getInputStream()) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return JsonUtil.parseObject(json, EvalSet.class);
        }
    }

    /** 仅系统内置文档（owner=0），保证 A/B 对照公平 */
    private String ownerFilter() {
        return String.format("%s == %d", CustomerDocMetadata.KEY_OWNER_USER_ID, CustomerDocMetadata.SYSTEM_OWNER_USER_ID);
    }

    private boolean hitAny(List<Document> docs, List<String> expect) {
        if (expect == null || expect.isEmpty()) {
            return false;
        }
        for (Document doc : docs) {
            String text = doc.getText();
            if (text == null) {
                continue;
            }
            for (String e : expect) {
                if (e != null && text.contains(e)) {
                    return true;
                }
            }
        }
        return false;
    }
}
