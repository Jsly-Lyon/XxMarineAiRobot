package com.hhuly.ai.robot.reader;

import com.hhuly.ai.robot.utils.PlainTextChunker;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 统一文档内容读取器：按扩展名选择“结构化优先、Tika 兜底”的解析策略。
 *
 * - .md/.markdown：MarkdownReader（标题切块、代码块单独成块）
 * - .txt/.text：按段落切块
 * - .html/.htm：Jsoup 结构化提取（按标题 h1~h6 切块、<pre>/<code> 代码整块保留）
 * - .pdf：PDFBox 按页提取（metadata 带 page），页内再段落切块
 * - .doc/.docx/.ppt/.pptx：暂用 Tika 抽取纯文本切块（后续 Word/PPT 换 POI 结构化解）
 *
 * @author: li
 * @date: 2026/9/3
 **/
@Component
@Slf4j
public class DocContentReader {

    /** 这些标签视为“独立块”，产生独立/续写的文本单元 */
    private static final Set<String> BLOCK_TAGS = Set.of(
            "h1", "h2", "h3", "h4", "h5", "h6",
            "p", "pre", "code", "li", "table", "blockquote", "hr");

    /** 需要继续向下展开的容器标签（避免把 <ul> 整包成一段） */
    private static final Set<String> WRAPPER_TAGS = Set.of(
            "body", "div", "section", "article", "main", "ul", "ol", "form");

    /** 代码块字符上限，超出按行切块保护语法连续性 */
    private static final int CODE_CHUNK_LINES = 200;

    private final MarkdownReader markdownReader;
    private final DocumentTextLoader tikaTextLoader;

    public DocContentReader(MarkdownReader markdownReader, DocumentTextLoader tikaTextLoader) {
        this.markdownReader = markdownReader;
        this.tikaTextLoader = tikaTextLoader;
    }

    /**
     * 解析文档为 Document 集合（结构化 chunk + 元数据）
     *
     * @param resource   文档资源
     * @param fileName   原始文件名（决定解析策略）
     * @param baseMeta   基础元数据（调用方提供，如 owner/mdStorageId/originalFileName）
     * @return 文档块集合
     */
    public List<Document> parse(Resource resource, String fileName, Map<String, Object> baseMeta) {
        String ext = extOf(fileName);
        Map<String, Object> meta = new HashMap<>(baseMeta == null ? Map.of() : baseMeta);
        meta.put("docType", ext.isBlank() ? "UNKNOWN" : ext.toUpperCase(Locale.ROOT));

        try {
            return switch (ext) {
                case "md", "markdown" -> markdownReader.loadMarkdown(resource, meta);
                case "txt", "text" -> PlainTextChunker.chunk(readText(resource), meta);
                case "html", "htm" -> parseHtml(resource, meta);
                case "pdf" -> parsePdf(resource, meta);
                // doc/docx/ppt/pptx 等暂以 Tika 兜底（后续 Word/PPT 换 POI）
                default -> PlainTextChunker.chunk(tikaTextLoader.extractText(resource), meta);
            };
        } catch (Exception ex) {
            log.error("## 文档内容读取失败: {} (ext={})", fileName, ext, ex);
            throw new IllegalArgumentException("文档解析失败：" + fileName, ex);
        }
    }

    /**
     * HTML：移除脚本/样式后，按标题层级切块；<pre>/<code> 代码整块保留
     */
    private List<Document> parseHtml(Resource resource, Map<String, Object> baseMeta) throws Exception {
        try (InputStream in = resource.getInputStream()) {
            org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(in, "UTF-8", "");
            jsoupDoc.select("script, style, noscript, svg, iframe").remove();

            Element body = jsoupDoc.body() != null ? jsoupDoc.body() : jsoupDoc;
            Elements blocks = new Elements();
            collectBlocks(body, blocks);

            List<Document> docs = new ArrayList<>();
            StringBuilder buffer = new StringBuilder();
            String currentHeading = null;

            for (Element block : blocks) {
                String tag = block.tagName();
                if (tag.matches("h[1-6]")) {
                    flushBuffer(docs, buffer, baseMeta, currentHeading, false);
                    currentHeading = block.text().trim();
                } else if ("pre".equals(tag) || "code".equals(tag)) {
                    // 代码块：当前普通文本先落块，代码单独整块保存（保留换行）
                    flushBuffer(docs, buffer, baseMeta, currentHeading, false);
                    addCodeBlock(docs, block.text(), baseMeta, currentHeading);
                } else {
                    String text = block.text().trim();
                    if (text.isEmpty()) {
                        continue;
                    }
                    if (buffer.length() > 0) {
                        buffer.append('\n');
                    }
                    buffer.append(text);
                    if (buffer.length() >= PlainTextChunker.TEXT_CHUNK_SIZE) {
                        flushBuffer(docs, buffer, baseMeta, currentHeading, false);
                    }
                }
            }
            flushBuffer(docs, buffer, baseMeta, currentHeading, false);
            return docs;
        }
    }

    /**
     * PDF：按页提取文本（metadata 带 page），扫描型/无文本页跳过（后续接 OCR）
     */
    private List<Document> parsePdf(Resource resource, Map<String, Object> baseMeta) throws Exception {
        List<Document> docs = new ArrayList<>();
        try (InputStream in = resource.getInputStream(); PDDocument pdf = PDDocument.load(in)) {
            int pages = pdf.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(pdf);
                if (text == null || text.isBlank()) {
                    // 无文本（纯图片/扫描 PDF），本阶段跳过，后续 OCR
                    log.info("## PDF 第 {} 页无文本，跳过（若为扫描件后续接入 OCR）", page);
                    continue;
                }
                Map<String, Object> pageMeta = new HashMap<>(baseMeta);
                pageMeta.put("page", page);
                docs.addAll(PlainTextChunker.chunk(text, pageMeta));
            }
        }
        return docs;
    }

    /**
     * 递归收集“块级”元素（跳过容器），保持文档顺序
     */
    private void collectBlocks(Element node, Elements out) {
        for (Element child : node.children()) {
            String tag = child.tagName();
            if (BLOCK_TAGS.contains(tag)) {
                out.add(child);
            } else if (WRAPPER_TAGS.contains(tag)) {
                collectBlocks(child, out);
            }
            // 其它内联/未知标签递归到块级再收集
            else {
                collectBlocks(child, out);
            }
        }
    }

    private void addCodeBlock(List<Document> docs, String code, Map<String, Object> baseMeta, String heading) {
        if (code == null || code.isBlank()) {
            return;
        }
        String[] lines = code.split("\\r?\\n", -1);
        StringBuilder chunk = new StringBuilder();
        int lineNo = 0;
        for (String line : lines) {
            if (chunk.length() > 0) {
                chunk.append('\n');
            }
            chunk.append(line);
            lineNo++;
            if (lineNo >= CODE_CHUNK_LINES || chunk.length() >= PlainTextChunker.TEXT_CHUNK_SIZE) {
                docs.add(buildDoc(chunk.toString(), baseMeta, heading));
                chunk.setLength(0);
                lineNo = 0;
            }
        }
        if (chunk.length() > 0) {
            docs.add(buildDoc(chunk.toString(), baseMeta, heading));
        }
    }

    private void flushBuffer(List<Document> docs, StringBuilder buffer,
                             Map<String, Object> baseMeta, String heading, boolean force) {
        String content = buffer.toString().trim();
        buffer.setLength(0);
        if (!content.isEmpty()) {
            docs.add(buildDoc(content, baseMeta, heading));
        }
    }

    private Document buildDoc(String text, Map<String, Object> baseMeta, String heading) {
        Map<String, Object> meta = new HashMap<>(baseMeta);
        if (heading != null && !heading.isBlank()) {
            meta.put("heading", heading);
        }
        return new Document(text.trim(), meta);
    }

    private String readText(Resource resource) throws Exception {
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        return idx < 0 ? "" : fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
