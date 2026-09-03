package com.hhuly.ai.robot.runner;

import com.hhuly.ai.robot.constant.CustomerDocMetadata;
import com.hhuly.ai.robot.reader.DocContentReader;
import com.hhuly.ai.robot.service.DocChunkStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.id.IdGenerator;
import org.springframework.ai.document.id.JdkSha256HexIdGenerator;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 系统内置固定文档：应用启动时自动读取并向量化到 Milvus。
 *
 * 扫描位置：classpath 下 customer-service.seed-docs-pattern（默认 doucument/MarineScience/**），
 * 覆盖 markdown / txt / word / ppt / pdf / html 等多格式子目录。
 *
 * 现支持解析：
 *  - .md / .markdown：复用 MarkdownReader（标题分块、保留元数据）
 *  - .txt / .text：按空行段落 + 超长切块
 *  其它格式（docx/ppt/pdf/html…）当前“识别到但跳过并告警”，待解析器接入后自动纳入。
 *
 * 幂等策略：与用户上传一致，按“内容哈希”生成稳定 Document ID；
 * 重复启动/重复内容写入相同 ID（覆盖而非新增）。
 *
 * @author: li
 * @date: 2026/9/3
 **/
@Component
@Slf4j
public class BuiltinDocsVectorizeRunner implements ApplicationRunner {

    /** 是否启用启动自动向量化 */
    @Value("${customer-service.seed-docs-enabled:true}")
    private boolean enabled;

    /** 内置固定文档扫描位置（classpath ant 模式） */
    @Value("${customer-service.seed-docs-pattern:classpath*:doucument/MarineScience/**}")
    private String seedDocsPattern;

    /** 当前支持的扩展名（md/txt 走专属逻辑，其余走 Tika 抽文） */
    private static final Set<String> SUPPORTED_EXTS = Set.of(
            "md", "markdown", "txt", "text",
            "doc", "docx", "ppt", "pptx", "pdf", "html", "htm");

    @jakarta.annotation.Resource
    private DocContentReader docContentReader;

    @jakarta.annotation.Resource
    private VectorStore vectorStore;

    @jakarta.annotation.Resource
    private DocChunkStore docChunkStore;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("## 已关闭内置文档启动自动向量化（customer-service.seed-docs-enabled=false）");
            return;
        }

        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(seedDocsPattern);
        } catch (Exception ex) {
            log.error("## 扫描内置文档目录失败, pattern={}", seedDocsPattern, ex);
            return;
        }

        if (resources.length == 0) {
            log.info("## 内置文档目录为空，无需向量化: {}", seedDocsPattern);
            return;
        }

        List<Resource> supported = new ArrayList<>();
        int unsupported = 0;
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            // 跳过占位资源（无文件名）
            if (fileName == null || fileName.isEmpty()) {
                continue;
            }
            String ext = extOf(fileName);
            if (SUPPORTED_EXTS.contains(ext)) {
                supported.add(resource);
            } else {
                unsupported++;
                log.warn("## 内置文档扩展名暂不支持，跳过: {}", fileName);
            }
        }

        if (supported.isEmpty()) {
            log.info("## 没有可向量化的内置文档（{} 个文件待后续接入解析器）", unsupported);
            return;
        }

        log.info("## 开始启动自动向量化：共 {} 个内置文档（另有 {} 个格式待接入）", supported.size(), unsupported);
        int success = 0;
        for (Resource resource : supported) {
            if (vectorizeResource(resource)) {
                success++;
            }
        }
        log.info("## 内置文档向量化结束：成功 {}/{}", success, supported.size());
    }

    /**
     * 向量化单个内置文档
     *
     * @return 是否成功（单个失败不阻断启动）
     */
    private boolean vectorizeResource(Resource resource) {
        String fileName = resource.getFilename();
        try {
            // 归属：系统内置（owner=0），所有登录用户可检索
            Map<String, Object> metadatas = new HashMap<>();
            metadatas.put(CustomerDocMetadata.KEY_OWNER_USER_ID, CustomerDocMetadata.SYSTEM_OWNER_USER_ID);
            metadatas.put(CustomerDocMetadata.KEY_SOURCE_TYPE, CustomerDocMetadata.SOURCE_TYPE_BUILTIN);
            metadatas.put("originalFileName", fileName);

            // 统一内容读取：按扩展名选择结构化/兜底解析策略
            List<Document> documents = docContentReader.parse(resource, fileName, metadatas);

            if (documents.isEmpty()) {
                log.warn("## 内置文档解析为空块，跳过: {}", fileName);
                return false;
            }

            // 内容哈希 -> 稳定 ID（幂等，覆盖而非新增）
            IdGenerator hashIdGenerator = new JdkSha256HexIdGenerator();
            List<Document> stableDocuments = documents.stream()
                    .map(doc -> Document.builder()
                            .text(doc.getText())
                            .metadata(doc.getMetadata())
                            .id(hashIdGenerator.generateId((Object) doc.getText()))
                            .build())
                    .toList();

            // 向量模型单批上限 10，分批写入 Milvus
            List<Document> batch = new ArrayList<>();
            for (Document doc : stableDocuments) {
                batch.add(doc);
                if (batch.size() == 10) {
                    vectorStore.add(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                vectorStore.add(batch);
            }

            // 同步文本块到关键词语料表（系统内置 owner=0）
            docChunkStore.syncChunks(stableDocuments, CustomerDocMetadata.SYSTEM_OWNER_USER_ID, null, fileName);

            log.info("## 内置文档向量化成功: {}（{} 块）", fileName, stableDocuments.size());
            return true;
        } catch (Exception ex) {
            log.error("## 内置文档向量化失败: {}", fileName, ex);
            return false;
        }
    }

    private String extOf(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx < 0 ? "" : fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
