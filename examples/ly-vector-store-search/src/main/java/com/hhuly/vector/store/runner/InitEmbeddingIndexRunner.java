package com.hhuly.vector.store.runner;

import com.hhuly.vector.store.reader.MyPdfReader;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.response.QueryResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 项目启动时，将指定 PDF 向量化并写入 Milvus 向量库。
 * 以内容 MD5 作为确定性主键，入库前查询已存在主键并跳过，实现幂等去重。
 */
@Component
public class InitEmbeddingIndexRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitEmbeddingIndexRunner.class);

    private final VectorStore vectorStore;
    private final MyPdfReader myPdfReader;
    private final MilvusClientV2 milvusClient;

    @Value("${document.pdf-path}")
    private String pdfPath;

    @Value("${spring.ai.vectorstore.milvus.collection-name}")
    private String collectionName;

    public InitEmbeddingIndexRunner(VectorStore vectorStore, MyPdfReader myPdfReader, MilvusClientV2 milvusClient) {
        this.vectorStore = vectorStore;
        this.myPdfReader = myPdfReader;
        this.milvusClient = milvusClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 1. 读取指定 PDF
            List<Document> documents = myPdfReader.getDocsFromPdf(pdfPath);
            if (documents.isEmpty()) {
                log.warn("PDF 无有效内容，跳过入库: {}", pdfPath);
                return;
            }

            // 2. 长文本按 token 分块，避免单条文本超出 embedding 输入上限
            List<Document> chunkedDocs = TokenTextSplitter.builder()
                    .withChunkSize(1000) // 每个分块的目标 token 数
                    .withMinChunkSizeChars(400) // 分块截断阶段的字符下限
                    .withMinChunkLengthToEmbed(10) // 小于此长度直接丢弃，避免污染
                    .withMaxNumChunks(5000) // 单个文档最多块数，防止超长文档块爆炸
                    .withKeepSeparator(true) // 分块边界保留分隔符
                    .build()
                    .apply(documents);
            log.info("PDF 分块完成，共 {} 个分块", chunkedDocs.size());

            // 3. 以内容 MD5 作为确定性主键，保证同一内容多次启动不重复入库
            // 注意：Document(String id, String text, Map) 第一个参数是 id
            List<Document> dedupDocs = chunkedDocs.stream()
                    .map(doc -> new Document(md5(doc.getText()), doc.getText(), doc.getMetadata()))
                    .toList();

            // 4. 查询已存在的主键，只写入新增内容
            Set<String> existingIds = queryExistingIds(dedupDocs);
            List<Document> toAdd = dedupDocs.stream()
                    .filter(doc -> !existingIds.contains(doc.getId()))
                    .toList();

            if (toAdd.isEmpty()) {
                log.info("PDF 内容已全部在库，跳过重复添加: {}", pdfPath);
                return;
            }

            // 5. 写入向量库（百炼 embedding 单次 batch 上限 10，分批提交）
            int batchSize = 10;
            for (int i = 0; i < toAdd.size(); i += batchSize) {
                List<Document> batch = toAdd.subList(i, Math.min(i + batchSize, toAdd.size()));
                vectorStore.add(batch);
            }
            log.info("向量化并写入完成，新增 {} 条，跳过重复 {} 条", toAdd.size(), dedupDocs.size() - toAdd.size());
        } catch (Exception e) {
            log.error("初始化向量库失败，请检查 PDF 路径与 Milvus/embedding 配置", e);
        }
    }

    /** 查询指定主键中已存在于 Milvus 的集合（首次启动 collection 不存在时视为无历史数据） */
    private Set<String> queryExistingIds(List<Document> documents) {
        List<String> ids = documents.stream().map(Document::getId).toList();
        if (ids.isEmpty()) {
            return Set.of();
        }
        try {
            // 按主键直接查询，避免 filter 语法问题
            QueryResp resp = milvusClient.query(QueryReq.builder()
                    .collectionName(collectionName)
                    .ids(new ArrayList<>(ids))
                    .outputFields(List.of("doc_id"))
                    .build());
            return resp.getQueryResults().stream()
                    .map(r -> String.valueOf(r.getEntity().get("doc_id")))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("查询已存在主键失败（首次启动 collection 未建属正常），按无历史数据处理: {}", e.getMessage());
            return Set.of();
        }
    }

    /** 计算内容 MD5 作为确定性主键 */
    private static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }
}
