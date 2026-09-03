package com.hhuly.ai.robot.event.listener;

import com.hhuly.ai.robot.event.ChatWindowRollEvent;
import com.hhuly.ai.robot.service.SessionRollService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 会话窗口滚动监听器：异步执行压缩/归档，不阻塞对话响应
 *
 * @author: li
 * @date: 2026/9/3
 **/
@Component
@Slf4j
public class SessionRollListener {

    @Resource
    private SessionRollService sessionRollService;

    @EventListener
    @Async("eventTaskExecutor") // 使用自定义事件线程池
    public void onRoll(ChatWindowRollEvent event) {
        log.info("## 收到会话窗口滚动事件, chatUuid = {}", event.getChatUuid());
        sessionRollService.roll(event.getChatUuid(), event.getUserId());
    }
}
