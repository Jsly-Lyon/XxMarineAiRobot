package com.hhuly.ai.robot.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Embedding 模型配置
 *
 * @author: li
 * @date: 2026/9/3
 * @description: 项目同时引入了 OpenAI 与 Ollama 两个 Embedding 自动配置，
 * 这里显式指定 embedding 统一使用本地 Ollama 的 BGE-M3（vectorStore / 后续检索都注入它）。
 **/
@Configuration
public class EmbeddingModelConfig {

    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(@Qualifier("ollamaEmbeddingModel") EmbeddingModel ollamaEmbeddingModel) {
        return ollamaEmbeddingModel;
    }
}
