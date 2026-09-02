package com.hhuly.ai.robot.controller;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Author: li
 * @Date: 2026/8/26 18:59
 * @Version: v1.0.0
 * @Description: 多模态
 **/
@RestController
@RequestMapping("/v9/ai")
public class MultimodalityController {
    @Resource
    private OpenAiChatModel chatModel;

    /**
     * 流式对话
     * @param message
     * @return
     */
    @GetMapping(value = "/generateStream", produces = "text/html;charset=utf-8")
    public Flux<String> generateStream(@RequestParam(value = "message") String message) {
        // 1. 创建媒体资源
        Media image = new Media(
                MimeTypeUtils.IMAGE_PNG,
                new ClassPathResource("/images/preview.jpg")
        );

        // 2. 附加选项（可选），如温度值等等
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("temperature", 0.7);

        // 3. 构建多模态消息
        UserMessage userMessage = UserMessage.builder()
                .text(message)
                .media(image)
                .metadata(metadata)
                .build();

        // 4. 构建提示词
        Prompt prompt = new Prompt(List.of(userMessage));
        // 5. 流式调用
        return chatModel.stream(prompt)
                .mapNotNull(chatResponse -> {
                    Generation generation = chatResponse.getResult();
                    return Objects.nonNull(generation) ? generation.getOutput().getText() : null;
                });

    }
}
