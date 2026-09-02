package com.hhuly.ai.robot.controller;

import com.hhuly.ai.robot.tools.DateTimeTools;
import com.hhuly.ai.robot.tools.WeatherTools;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @Author: 李杨
 * @Date: 2026/5/23 18:25
 * @Version: v1.0.0
 * @Description: Chat Client 客户端
 **/
@RestController
@RequestMapping("/v2/ai")
public class ChatClientController {
    @Resource
    private ChatClient chatClient;

    /**
     * 普通对话
     * @param message
     * @return
     */
    @GetMapping("/generate")
    public String generate(@RequestParam(value = "message", defaultValue = "你是谁？") String message,
                           @RequestParam(value = "chatId") String chatId) {
        // 一次性返回结果
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId)) // 对话 ID
                .call()
                .content();
    }

    /**
     * 流式对话
     * @param message
     * @return
     */
    @GetMapping(value = "/generateStream", produces = "text/html;charset=utf-8")
    public Flux<String> generateStream(@RequestParam(value = "message", defaultValue = "你是谁？") String message,
                                       @RequestParam(value = "chatId") String chatId) {
        return chatClient.prompt()
                // .system("请你扮演一名 Java 项目实战专栏的客服人员")
                .tools(new DateTimeTools(), new WeatherTools()) // Function Call
                .user(message) // 提示词
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId)) // 对话 ID
                .stream() // 流式输出
                .content();
    }
}
