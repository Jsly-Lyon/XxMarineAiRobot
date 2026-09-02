package com.hhuly.ai.robot.controller;

import org.apache.commons.lang3.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: 李杨
 * @Date: 2026/6/23 13:40
 * @Version: v1.0.0
 * @Description: DeepSeek-V4-Pro 聊天（推理大模型，输出思维链）
 **/
@RestController
@RequestMapping("/v1/ai")
public class DeepSeekReasonChatController {

    @Resource
    private DeepSeekChatModel deepSeekChatModel;

    /**
     * 流式对话（含推理过程）
     * @param message
     * @return
     */
    @GetMapping(value = "/generateStream", produces = "text/html;charset=utf-8")
    public Flux<String> generateStream(@RequestParam(value = "message", defaultValue = "你是谁?")String message){
        // TODO: 对接前端时优化——当前以 text/html 直接输出 HTML 片段（<span>/<br/> 等标签），需配合前端 innerHTML 渲染；
        //  后续建议改造为 SSE(text/event-stream) 输出结构化事件（区分 reasoning/text），由前端 textContent 渲染，样式交由前端 CSS 控制

        // 构建模型请求的选项对象，设置目标模型为deepseek-v4-pro
        DeepSeekChatOptions chatOptions = DeepSeekChatOptions.builder()
                .model(DeepSeekApi.ChatModel.DEEPSEEK_V4_PRO.getValue())
                .build();

        // 构建提示词
        Prompt prompt = new Prompt(message,chatOptions);

        // 使用原子布尔值跟踪分隔线状态（每个请求独立）
        AtomicBoolean needSeparator = new AtomicBoolean(true);

        // 流式输出
        return deepSeekChatModel.stream(prompt)
                .mapNotNull(chatResponse -> {
                    // 获取相应内容，强转为DeepSeek专属的消息对象，以便拿到推理内容
                    DeepSeekAssistantMessage deepSeekAssistantMessage = (DeepSeekAssistantMessage) chatResponse.getResult().getOutput();

                    // 获取推理内容
                    String reasoningContent = deepSeekAssistantMessage.getReasoningContent();

                    // 推理结束后的正式回答
                    String text = deepSeekAssistantMessage.getText();

                    // 是否是正式回答
                    boolean isTextResponse = StringUtils.isNotBlank(text);
                    // 推理结束后输出正式回答，否则输出推理过程
                    String rawContent = isTextResponse ? text : reasoningContent;

                    // 无内容片段直接跳过，交由 mapNotNull 过滤
                    if (StringUtils.isBlank(rawContent)) {
                        return null;
                    }

                    // 将 \n 替换为 HTML 换行标签 <br>，让浏览器能识别换行
                    String processed = rawContent.replace("\n", "<br/>");

                    StringBuilder fragment = new StringBuilder();
                    // 在正式回答内容之前，插入一条 <hr> 分割线
                    if (isTextResponse && needSeparator.compareAndExchange(true, false)) {
                        fragment.append("<hr/>");
                    }

                    if (isTextResponse) {
                        // 正式回答：正常字号、深色正文
                        fragment.append("<span style=\"color:#333;font-size:15px;\">").append(processed).append("</span>");
                    } else {
                        // 推理过程：灰色、斜体、小号字体，与正式回答明显区分
                        fragment.append("<span style=\"color:#8c8c8c;font-size:13px;font-style:italic;\">").append(processed).append("</span>");
                    }
                    return fragment.toString();
                });

    }
}
