package com.hhuly.ai.robot.reader;

import cn.hutool.core.collection.CollUtil;
import com.hhuly.ai.robot.utils.MarkdownChunker;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Markdown 文档读取器
 *
 * @author: li
 * @date: 2026/9/3
 * @description: 将 Markdown 文件读取为 Spring AI Document 集合。
 * 复用 MarkdownChunker 按标题层级分块并保留代码块，每块携带标题路径(titlePath)便于溯源。
 **/
@Component
public class MarkdownReader {

    /**
     * 读取 Markdown 文件为文档集合
     * @param resource
     * @param metadatas
     * @return
     */
    public List<Document> loadMarkdown(Resource resource, Map<String, Object> metadatas) {
        // MarkdownDocumentReader 阅读器配置类
        MarkdownDocumentReaderConfig.Builder configBuilder = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true) // 遇到水平线 ---，则创建新文档
                .withIncludeCodeBlock(false) // 排除代码块（代码块生成单独文档）
                .withIncludeBlockquote(false); // 排除块引用（块引用生成单独文档）

        // 添加自定义元数据，如文件名称
        if (CollUtil.isNotEmpty(metadatas)) {
            configBuilder.withAdditionalMetadata(metadatas);
        }

        // 新建 MarkdownDocumentReader 阅读器
        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, configBuilder.build());

        // 读取并转换为 Document 文档集合
        return reader.get();
    }
}
