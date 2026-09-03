package com.hhuly.ai.robot.utils;

import com.huaban.analysis.jieba.JiebaSegmenter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文分词工具（jieba）
 *
 * @author: li
 * @date: 2026/9/3
 * @description: 提供两套输出：空格词串（写入 PG 生成 tsvector）与 "&" 连接词串（构造 PG tsquery）。
 **/
public final class ChineseTokenizer {

    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();

    private ChineseTokenizer() {
    }

    /**
     * 分词为空格分隔的词串，用于存储：to_tsvector('simple', tokens)
     */
    public static String toSpaceText(String text) {
        return String.join(" ", tokenize(text));
    }

    /**
     * 分词为 " & " 连接的词串，用于检索：to_tsquery('simple', tsQuery)
     * 无有效词时返回 null（调用方跳过关键词路）
     */
    public static String toAndTsQuery(String text) {
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return null;
        }
        return String.join(" & ", tokens);
    }

    /**
     * jieba 分词并过滤空白词
     */
    public static List<String> tokenize(String text) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return result;
        }
        for (String word : SEGMENTER.sentenceProcess(text)) {
            if (StringUtils.hasText(word) && !word.isBlank()) {
                result.add(word.trim());
            }
        }
        return result;
    }
}
