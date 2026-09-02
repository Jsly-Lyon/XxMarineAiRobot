package com.hhuly.ai.robot.config;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.cassandra.CassandraChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    /**
     * 记忆存储
     */
    @Resource
    private CassandraChatMemoryRepository chatMemoryRepository;

    /**
     * 配置记忆存储:初始化一个ChatMemory实例，并注入到Spring容器中
     * @return
     */
    @Bean
    public ChatMemory chatMemory(){
        return MessageWindowChatMemory.builder()
                .maxMessages(50) // 最大消息窗口为 50，默认值为 20
                .chatMemoryRepository(chatMemoryRepository) // 记忆存储
                .build();
    }

}
