package com.hhuly.ai.robot.utils;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 分块工具
 *
 * @author: li
 * @date: 2026/9/3
 * @description: 将 Markdown 文档按标题层级切成块；代码块整体保留不拆散，每块携带标题路径便于溯源。
 * 后续 Milvus 转接时，每块作为一个待向量化/入库的单元。
 **/
public final class MarkdownChunker {

    /** 单块目标最大字符数 */
    private static final int MAX_LEN = 800;

    /** 标题正则：# ~ #### */
    private static final String HEADING_REGEX = "^(#{1,4})\\s+(.*)$";

    private MarkdownChunker() {
    }

    /** 分块结果：内容 + 标题路径（溯源用） */
    public record Chunk(String content, String titlePath) {
    }

    public static List<Chunk> chunk(String content) {
        List<Chunk> chunks = new ArrayList<>();
        if (!StringUtils.hasText(content)) {
            return chunks;
        }

        String[] titleStack = new String[5]; // index 1~4 对应 # ~ #### 最近一次标题
        StringBuilder buffer = new StringBuilder();
        boolean inCode = false;

        for (String line : content.split("\\R")) {
            String trimmed = line.trim();

            // 代码块开关：整块保留在缓冲区，不参与分块
            if (trimmed.startsWith("```")) {
                inCode = !inCode;
            }

            if (!inCode && trimmed.matches(HEADING_REGEX)) {
                // 遇到标题：先把当前缓冲落一个块，再切换标题上下文
                flushIfNeeded(buffer, chunks, titleStack);
                int level = trimmed.indexOf(' ');
                int hashCount = countHash(trimmed);
                String titleText = trimmed.substring(hashCount).trim();
                // 同级别及更低级别标题清空（回退标题栈）
                for (int i = hashCount + 1; i < titleStack.length; i++) {
                    titleStack[i] = null;
                }
                titleStack[hashCount] = titleText;
                continue;
            }

            appendLine(buffer, line, chunks, titleStack);
        }
        flushIfNeeded(buffer, chunks, titleStack);

        return chunks;
    }

    private static int countHash(String headingLine) {
        int n = 0;
        while (n < headingLine.length() && headingLine.charAt(n) == '#') {
            n++;
        }
        return n;
    }

    private static String currentTitlePath(String[] titleStack) {
        StringBuilder sb = new StringBuilder();
        for (String title : titleStack) {
            if (title != null) {
                if (!sb.isEmpty()) {
                    sb.append(" > ");
                }
                sb.append(title);
            }
        }
        return sb.toString();
    }

    private static void appendLine(StringBuilder buffer, String line, List<Chunk> chunks, String[] titleStack) {
        buffer.append(line).append('\n');
        // 超出单块上限：就近换行截断落块
        if (buffer.length() >= MAX_LEN) {
            flush(buffer, chunks, currentTitlePath(titleStack), false);
        }
    }

    private static void flushIfNeeded(StringBuilder buffer, List<Chunk> chunks, String[] titleStack) {
        flush(buffer, chunks, currentTitlePath(titleStack), true);
    }

    /**
     * 落块。若 keepRemainder=true 但 buffer 过长，会强制截断一次（尽量保留整行）。
     */
    private static void flush(StringBuilder buffer, List<Chunk> chunks, String titlePath, boolean allowTruncate) {
        if (buffer.isEmpty()) {
            return;
        }
        if (buffer.length() > MAX_LEN * 2 && allowTruncate) {
            // 超长（多为无标题长文本），按 MAX_LEN 就近换行硬切
            String all = buffer.toString();
            buffer.setLength(0);
            int start = 0;
            while (start < all.length()) {
                int end = Math.min(all.length(), start + MAX_LEN);
                int newline = all.lastIndexOf('\n', end);
                if (newline > start) {
                    end = newline;
                }
                chunks.add(new Chunk(all.substring(start, end).trim(), titlePath));
                start = end + 1;
            }
            return;
        }
        chunks.add(new Chunk(buffer.toString().trim(), titlePath));
        buffer.setLength(0);
    }
}
