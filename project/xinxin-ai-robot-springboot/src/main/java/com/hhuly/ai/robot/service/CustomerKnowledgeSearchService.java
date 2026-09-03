package com.hhuly.ai.robot.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 客服知识检索服务（双路：Dense + 关键词 BM25 -> RRF 融合）
 *
 * @author: li
 * @date: 2026/9/3
 **/
public interface CustomerKnowledgeSearchService {

    /**
     * 检索与问题最相关的文档块
     *
     * @param query  用户问题
     * @param topK   返回条数
     * @param userId 当前登录用户（数据隔离：仅内置 + 本人）；null 时仅内置
     * @return 文档块（含 metadata：owner / 来源）
     */
    List<Document> search(String query, int topK, Long userId);
}
