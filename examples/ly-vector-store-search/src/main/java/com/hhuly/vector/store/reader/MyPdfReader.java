package com.hhuly.vector.store.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 文档读取器：自动扫描 classpath:/document/ 目录下的所有 PDF（含子目录），
 * 新增 PDF 只需放入该目录，无需修改代码
 */
@Component
public class MyPdfReader {

    private static final Logger log = LoggerFactory.getLogger(MyPdfReader.class);

    /** PDF 文件位置（classpath 下），** 表示递归匹配子目录 */
    private static final String PDF_PATTERN = "classpath:/document/**/*.pdf";

    private final PdfDocumentReaderConfig readerConfig = PdfDocumentReaderConfig.builder()
            .withPageTopMargin(0) // 设置页面顶边距为0
            .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                    .withNumberOfTopTextLinesToDelete(0) // 设置删除顶部文本行数为0
                    .build())
            .withPagesPerDocument(1) // 设置每个文档包含1页
            .build();

    /** 读取指定路径的单个 PDF，返回文档集合 */
    public List<Document> getDocsFromPdf(String pdfPath) {
        Resource resource = new PathMatchingResourcePatternResolver().getResource(pdfPath);
        if (!resource.exists()) {
            throw new IllegalStateException("PDF 文件不存在: " + pdfPath);
        }
        try {
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, readerConfig);
            List<Document> docs = pdfReader.read();
            String sourceName = resource.getFilename();
            docs.forEach(doc -> doc.getMetadata().put("source", sourceName));
            log.info("已读取 PDF: {}（{} 页）", sourceName, docs.size());
            return docs;
        } catch (Exception e) {
            throw new IllegalStateException("读取 PDF 失败: " + pdfPath, e);
        }
    }

    /** 读取 classpath:/document/ 目录下所有 PDF，返回合并后的文档集合 */
    public List<Document> getDocsFromPdf() {
        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(PDF_PATTERN);
        } catch (IOException e) {
            throw new IllegalStateException("扫描 PDF 目录失败: " + PDF_PATTERN, e);
        }

        if (resources.length == 0) {
            log.warn("未在 {} 下找到任何 PDF 文件", PDF_PATTERN);
            return List.of();
        }

        List<Document> allDocs = new ArrayList<>();
        for (Resource resource : resources) {
            try {
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, readerConfig);
                List<Document> docs = pdfReader.read();
                // 为每个文档标记来源文件名，便于检索结果溯源
                String sourceName = resource.getFilename();
                docs.forEach(doc -> doc.getMetadata().put("source", sourceName));
                allDocs.addAll(docs);
                log.info("已读取 PDF: {}（{} 页）", sourceName, docs.size());
            } catch (Exception e) {
                // 单个 PDF 读取失败不影响其他文件
                log.error("读取 PDF 失败: {}", resource.getFilename(), e);
            }
        }
        return allDocs;
    }
}
