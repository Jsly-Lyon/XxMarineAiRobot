package com.hhuly.ai.robot.controller;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @Author: 李杨
 * @Date: 2026/6/23 13:40
 * @Version: v1.0.0
 * @Description: DeepSeek-V4-Flash聊天
 **/
@RestController
@RequestMapping("/ai")
public class DeepSeekChatController {

    @Resource
    private DeepSeekChatModel deepSeekChatModel;

    @RequestMapping("/generate")
    public String generate(@RequestParam(value = "message", defaultValue = "你是谁")String message){
        return deepSeekChatModel.call(message);
    }

    @GetMapping(value = "/generateStream", produces = "text/html;charset=utf-8")
    public Flux<String> generateStream(@RequestParam(value = "message", defaultValue = "你是谁")String message){
        // 构建用户提示词
        Prompt prompt = new Prompt(new UserMessage(message));

        // 流式输出
        return deepSeekChatModel.stream(prompt)
                .mapNotNull(chatResponse -> chatResponse.getResult().getOutput().getText());
    }
}
