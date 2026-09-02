package com.hhuly.ai.robot.advisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * @Author: 李杨
 * @Date: 2026/5/23 18:45
 * @Version: v1.0.0
 * @Description: 自定义日志记录功能
 * 同时实现CallAdvisor（同步）和StreamAdvisor（流式）接口
 **/
@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    /**
     * 日志 JSON 序列化器（注册 JavaTimeModule 以支持 java.time.Duration 等类型的序列化）
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String getName() {
        // 获取类名称
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 1; // order 值越小，越先执行
    }
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.info("===== 同步调用（call）日志开始 =====");
        // 打印请求信息
        logRequest(chatClientRequest);

        // 执行下一个Advisor（或核心调用逻辑）
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        // 打印响应信息
        logResponse(chatClientResponse);
        log.info("===== 同步调用（call）日志结束 =====\n");

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        // 打印请求信息
        logRequest(chatClientRequest);

        // 执行下一个Advisor，获取流式响应
        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);

        // 聚合流式响应并打印（避免逐行打印碎片化日志）
        return new ChatClientMessageAggregator()
                .aggregateChatClientResponse(chatClientResponses, this::logResponse)
                .doFinally(signalType -> log.info("===== 流式调用（stream）日志结束 =====\n"));
    }

    /**
     * 打印请求详情
     */
    private void logRequest(ChatClientRequest request) {
        log.info("""
                ===== AI请求内容 =====
                {}
                """, toPrettyJson(request));
    }

    /**
     * 打印响应详情
     */
    private void logResponse(ChatClientResponse chatClientResponse) {
        log.info("""
                ===== AI响应内容 =====
                {}
                """, toPrettyJson(chatClientResponse));
    }

    /**
     * 将对象序列化为多行（pretty）JSON，分行展示便于阅读；
     * 序列化失败（如循环引用）时退化为 toString()
     */
    private String toPrettyJson(Object obj) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("对象序列化为 JSON 失败，退化为 toString()：{}", e.getMessage());
            return String.valueOf(obj);
        }
    }
}
