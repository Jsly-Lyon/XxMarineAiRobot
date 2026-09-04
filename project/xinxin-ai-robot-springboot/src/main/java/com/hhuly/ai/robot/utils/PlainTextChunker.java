package com.hhuly.ai.robot.utils;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 纯文本切块工具：按空行段落切分，超长段落再按窗口二次切分，生成 Spring AI Document。
 *
 * @author: li
 * @date: 2026/9/3
 **/
public final class PlainTextChunker {

    /** 单个 Document 文本的字符长度上限 */
    public static final int TEXT_CHUNK_SIZE = 1500;

    /** 相邻块的字符重叠比例（15%），用于超长段落二次切分时保持上下文衔接 */
    public static final double OVERLAP_RATIO = 0.15;

    private PlainTextChunker() {
    }

    /**
     * 将纯文本切分为文档集合（每块复制一份元数据）
     */
    public static List<Document> chunk(String text, Map<String, Object> metadatas) {
        List<Document> documents = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return documents;
        }

        StringBuilder paragraph = new StringBuilder();
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) {
                addParagraph(documents, paragraph, metadatas);
                paragraph.setLength(0);
            } else {
                if (paragraph.length() > 0) {
                    paragraph.append('\n');
                }
                paragraph.append(line.trim());
            }
        }
        addParagraph(documents, paragraph, metadatas);
        return documents;
    }

    private static void addParagraph(List<Document> documents, StringBuilder paragraph, Map<String, Object> metadatas) {
        String content = paragraph.toString().trim();
        if (content.isEmpty()) {
            return;
        }
        if (content.length() <= TEXT_CHUNK_SIZE) {
            documents.add(new Document(content, new HashMap<>(metadatas)));
            return;
        }
        // 超过上限：按固定窗口 + 15% 重叠滑动切分，保持相邻块上下文衔接
        int overlap = (int) Math.round(TEXT_CHUNK_SIZE * OVERLAP_RATIO);
        int stride = TEXT_CHUNK_SIZE - overlap; // 每次前进的步长
        int start = 0;
        int n = content.length();
        while (start < n) {
            int end = Math.min(n, start + TEXT_CHUNK_SIZE);
            String segment = content.substring(start, end).trim();
            if (!segment.isEmpty()) {
                documents.add(new Document(segment, new HashMap<>(metadatas)));
            }
            if (end >= n) {
                break;
            }
            start += stride; // 下一块起点前移 stride，与前一块重叠 overlap
        }
    }
}
