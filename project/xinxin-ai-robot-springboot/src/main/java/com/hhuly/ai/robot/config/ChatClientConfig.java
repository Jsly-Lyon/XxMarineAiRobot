package com.hhuly.ai.robot.config;

import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 李杨
 * @Date: 2026/5/23 18:20
 * @Version: v1.0.0
 * @Description: ChatClient 客户端配置
 **/
/// 使用更高层的ChatClient可以解耦合
@Configuration
public class ChatClientConfig {

    /**
     * 初始化 ChatClient 客户端
     * @param chatModel
     * @return
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .build();
    }
}
