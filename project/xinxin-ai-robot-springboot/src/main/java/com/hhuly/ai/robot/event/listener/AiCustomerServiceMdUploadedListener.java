package com.hhuly.ai.robot.event.listener;

import com.google.common.collect.Lists;
import com.hhuly.ai.robot.constant.CustomerDocMetadata;
import com.hhuly.ai.robot.domain.dos.AiCustomerServiceMdStorageDO;
import com.hhuly.ai.robot.domain.mapper.AiCustomerServiceMdStorageMapper;
import com.hhuly.ai.robot.enums.AiCustomerServiceMdStatusEnum;
import com.hhuly.ai.robot.event.AiCustomerServiceMdUploadedEvent;
import com.hhuly.ai.robot.reader.DocContentReader;
import com.hhuly.ai.robot.service.DocChunkStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.id.IdGenerator;
import org.springframework.ai.document.id.JdkSha256HexIdGenerator;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: li
 * @Date: 2026/9/2 23:56
 * @Version: v1.0.0
 * @Description: 问答文件上传事件监听器：异步读取 Markdown -> 分块为 Document -> BGE-M3 向量化 -> 写入 Milvus
 **/
@Component
@Slf4j
public class AiCustomerServiceMdUploadedListener {

    @Resource
    private DocContentReader docContentReader;
    @Resource
    private VectorStore vectorStore; // Spring AI 自动装配的 MilvusVectorStore
    @Resource
    private AiCustomerServiceMdStorageMapper mdStorageMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private DocChunkStore docChunkStore;

    /**
     * Markdown 文件向量化
     *
     * @param event 上传事件（含 fileId / filePath / metadatas）
     */
    @EventListener
    @Async("eventTaskExecutor") // 使用自定义事件线程池，不阻塞上传接口
    public void vectorizing(AiCustomerServiceMdUploadedEvent event) {
        Long fileId = event.getFileId();
        log.info("## 收到问答文件上传事件, fileId = {}, filePath = {}", fileId, event.getFilePath());

        // 更新状态为「向量化中」
        updateStatus(fileId, AiCustomerServiceMdStatusEnum.VECTORIZING, null);

        // 记录失败原因，便于失败后写入 remark
        AtomicReference<String> errorMsg = new AtomicReference<>();

        boolean success = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 1. 读取并分块（按扩展名选择解析器）
                AiCustomerServiceMdStorageDO storage = mdStorageMapper.selectById(fileId);
                String originalFileName = storage != null ? storage.getOriginalFileName() : event.getFilePath();

                Map<String, Object> metadatas = new HashMap<>();
                metadatas.put("mdStorageId", fileId); // 溯源 + 按文件过滤
                // 归属用户：检索时按 “内置 or 本人” 过滤，实现严格隔离
                if (storage != null && storage.getUserId() != null) {
                    metadatas.put(CustomerDocMetadata.KEY_OWNER_USER_ID, storage.getUserId());
                }
                metadatas.put("originalFileName", originalFileName);

                FileSystemResource resource = new FileSystemResource(event.getFilePath());
                // 统一内容读取：按扩展名选结构化/兜底解析（md/html/pdf 结构化，word/ppt 暂 Tika）
                List<Document> documents = docContentReader.parse(resource, originalFileName, metadatas);
                log.info("## 读取到 {} 个文档块, 文件= {}", documents.size(), originalFileName);

                // 2. 内容哈希 -> 稳定 ID：同一份文件重复上传落到同一 ID，天然幂等（覆盖而非新增）
                IdGenerator hashIdGenerator = new JdkSha256HexIdGenerator();
                List<Document> stableDocuments = documents.stream()
                        .map(doc -> Document.builder()
                                .text(doc.getText())
                                .metadata(doc.getMetadata())
                                .id(hashIdGenerator.generateId((Object) doc.getText()))
                                .build())
                        .toList();

                // 3. 向量模型限单批 10 条，分批写入 Milvus
                for (List<Document> batch : Lists.partition(stableDocuments, 10)) {
                    vectorStore.add(batch);
                }

                // 4. 同步文本块到关键词语料表（jieba 分词）
                docChunkStore.syncChunks(stableDocuments,
                        storage != null ? storage.getUserId() : null,
                        fileId,
                        storage != null ? storage.getOriginalFileName() : null);

                updateStatus(fileId, AiCustomerServiceMdStatusEnum.COMPLETED, null);
                return true;
            } catch (Exception ex) {
                log.error("## Markdown 文件向量化失败: {}", event, ex);
                errorMsg.set(ex.getMessage());
                status.setRollbackOnly(); // 标记事务回滚（本次不做任何写入）
                return false;
            }
        }));

        // 事务执行失败：更新文件状态为「失败」
        if (!success) {
            String msg = errorMsg.get();
            updateStatus(fileId, AiCustomerServiceMdStatusEnum.FAILED,
                    msg != null && msg.length() > 190 ? msg.substring(0, 190) : msg);
        }
    }

    private void updateStatus(Long fileId, AiCustomerServiceMdStatusEnum statusEnum, String remark) {
        AiCustomerServiceMdStorageDO update = AiCustomerServiceMdStorageDO.builder()
                .id(fileId)
                .status(statusEnum.getCode())
                .updateTime(LocalDateTime.now())
                .build();
        if (remark != null) {
            update.setRemark(remark);
        }
        mdStorageMapper.updateById(update);
    }
}
