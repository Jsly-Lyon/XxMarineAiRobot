package com.hhuly.ai.robot.reader;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.StringWriter;

/**
 * 基于 Apache Tika 的多格式文档纯文本抽取器。
 * 覆盖 doc/docx/ppt/pptx/pdf/html 等；Markdown 仍走 MarkdownReader（结构化分块更好）。
 *
 * @author: li
 * @date: 2026/9/3
 **/
@Component
@Slf4j
public class DocumentTextLoader {

    /**
     * 从任意支持的文档中抽取纯文本
     *
     * @param resource 文档资源
     * @return 抽取出的纯文本（可能为空）
     */
    public String extractText(Resource resource) {
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        StringWriter writer = new StringWriter();
        try (InputStream in = resource.getInputStream()) {
            parser.parse(in, new BodyContentHandler(writer), metadata, context);
            return writer.toString();
        } catch (Exception ex) {
            log.error("## 文档文本抽取失败: {}", resource.getFilename(), ex);
            throw new IllegalArgumentException("文档解析失败：" + resource.getFilename(), ex);
        }
    }
}
