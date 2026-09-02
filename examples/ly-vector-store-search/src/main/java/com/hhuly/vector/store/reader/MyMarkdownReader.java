package com.hhuly.vector.store.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: 李杨
 * @Date: 2026/3/21 16:09
 * @Version: v1.0.0
 * @Description: Markdown 文件读取
 **/
@Component
public class MyMarkdownReader {

    @Value("classpath:/document/code.md")
    private Resource resource;

    public List<Document> loadMarkdown() {
        // MarkdownDocumentReader 阅读器配置类
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true) // 遇到水平线 ---，则创建新文档
                .withIncludeCodeBlock(false) // 排除代码块（代码块生成单独文档）
                .withIncludeBlockquote(false) // 排除块引用（块引用生成单独文档）
                .withAdditionalMetadata("filename", "code.md") // 添加自定义元数据，如文件名称
                .build();

        // 新建 MarkdownDocumentReader 阅读器
        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);

        // 读取并转换为 Document 文档集合
        return reader.get();
    }
}
