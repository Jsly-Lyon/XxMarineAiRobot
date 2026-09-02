package com.hhuly.ai.robot.config;

import com.hhuly.ai.robot.advisor.MyLoggerAdvisor;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
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

    @Resource
    private ChatMemory chatMemory;
    /**
     * 创建 ChatClient
     * @param deepSeekChatModel
     * @return
     */
    @Bean
    public ChatClient chatClient(DeepSeekChatModel deepSeekChatModel, ToolCallbackProvider tools){
        return ChatClient.builder(deepSeekChatModel)
                .defaultTools(tools) // MCP
//                .defaultSystem("请你扮演一名优蓝云聊项目的克服人员")
                .defaultAdvisors(// new SimpleLoggerAdvisor(), //  添加 Spring AI 内置的日志记录功能
                                 new MyLoggerAdvisor(), // 添加自定义的日志打印 Advisor(同步输出和流式输出)
                                 MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
