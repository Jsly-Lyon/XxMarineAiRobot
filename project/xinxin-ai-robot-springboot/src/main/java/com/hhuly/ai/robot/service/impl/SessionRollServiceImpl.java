package com.hhuly.ai.robot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hhuly.ai.robot.domain.dos.ChatMessageDO;
import com.hhuly.ai.robot.domain.dos.SessionMemoryDO;
import com.hhuly.ai.robot.domain.mapper.ChatMessageMapper;
import com.hhuly.ai.robot.domain.mapper.SessionMemoryMapper;
import com.hhuly.ai.robot.service.SessionRollService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 会话窗口滚动实现
 *
 * @author: li
 * @date: 2026/9/3
 **/
@Slf4j
@Service
public class SessionRollServiceImpl implements SessionRollService {

    /** 窗口上限（活跃消息数） */
    private static final int WINDOW_MAX = 50;
    /** 单次压缩批大小 */
    private static final int COMPRESS_BATCH = 20;
    /** 防止死循环的最大迭代 */
    private static final int MAX_ITER = 10;
    /** 压缩摘要消息角色（写入 t_chat_message.role，注入时前置） */
    private static final String SUMMARY_ROLE = "summary";

    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private SessionMemoryMapper sessionMemoryMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    /** 摘要压缩走 OpenAI(阿里云 MaaS) 对话模型，避免与 ollamaChatModel 二义 */
    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Override
    public void roll(String chatUuid, Long userId) {
        if (!StringUtils.hasText(chatUuid)) {
            return;
        }
        try {
            transactionTemplate.execute(status -> {
                doRoll(chatUuid, userId);
                return true;
            });
        } catch (Exception e) {
            log.error("## 会话窗口滚动失败, chatUuid = {}", chatUuid, e);
        }
    }

    private void doRoll(String chatUuid, Long userId) {
        for (int iter = 0; iter < MAX_ITER; iter++) {
            // 取窗口内活跃消息（id 升序 = 时间顺序）
            List<ChatMessageDO> active = chatMessageMapper.selectList(
                    Wrappers.<ChatMessageDO>lambdaQuery()
                            .eq(ChatMessageDO::getChatUuid, chatUuid)
                            .eq(ChatMessageDO::getArchived, 0)
                            .orderByAsc(ChatMessageDO::getId));

            if (active.size() <= WINDOW_MAX) {
                return;
            }

            // 1) 尝试把最旧的普通消息压缩成一条摘要"放回窗口"
            List<ChatMessageDO> compressBatch = oldestOrdinary(active, COMPRESS_BATCH);
            if (!compressBatch.isEmpty()) {
                String summary = compress(compressBatch);
                chatMessageMapper.insert(ChatMessageDO.builder()
                        .chatUuid(chatUuid)
                        .content(summary)
                        .role(SUMMARY_ROLE)
                        .archived(0)
                        .createTime(LocalDateTime.now())
                        .build());
                archiveAll(compressBatch);
                log.info("## 窗口滚动-压缩: 会话 {} 压入 {} 条普通消息为摘要", chatUuid, compressBatch.size());
                continue;
            }

            // 2) 没有可再压的普通消息（活跃全是摘要且仍超窗）→ 最旧部分持久化到长期记忆
            int excess = active.size() - WINDOW_MAX;
            if (excess <= 0) {
                return;
            }
            List<ChatMessageDO> toArchive = new ArrayList<>(active.subList(0, Math.min(excess, active.size())));
            String memoryText = toArchive.stream()
                    .map(m -> "[" + m.getRole() + "] " + m.getContent())
                    .collect(Collectors.joining("\n"));
            int seq = sessionMemoryMapper.selectCount(Wrappers.<SessionMemoryDO>lambdaQuery()
                    .eq(SessionMemoryDO::getChatUuid, chatUuid)).intValue() + 1;
            sessionMemoryMapper.insert(SessionMemoryDO.builder()
                    .chatUuid(chatUuid)
                    .userId(userId)
                    .memory(memoryText)
                    .seq(seq)
                    .fromMsgId(toArchive.get(0).getId())
                    .toMsgId(toArchive.get(toArchive.size() - 1).getId())
                    .createTime(LocalDateTime.now())
                    .build());
            archiveAll(toArchive);
            log.info("## 窗口滚动-归档: 会话 {} 持久化 {} 条到 t_session_memory, seq={}",
                    chatUuid, toArchive.size(), seq);
        }
    }

    private List<ChatMessageDO> oldestOrdinary(List<ChatMessageDO> active, int size) {
        List<ChatMessageDO> batch = new ArrayList<>();
        for (ChatMessageDO msg : active) {
            if (batch.size() >= size) {
                break;
            }
            if (isOrdinary(msg)) {
                batch.add(msg);
            }
        }
        return batch;
    }

    private boolean isOrdinary(ChatMessageDO msg) {
        return Objects.equals(msg.getRole(), MessageType.USER.getValue())
                || Objects.equals(msg.getRole(), MessageType.ASSISTANT.getValue());
    }

    private void archiveAll(List<ChatMessageDO> list) {
        for (ChatMessageDO msg : list) {
            ChatMessageDO update = ChatMessageDO.builder()
                    .id(msg.getId())
                    .archived(1)
                    .build();
            chatMessageMapper.updateById(update);
        }
    }

    /**
     * 压缩一批消息为摘要；模型调用失败时降级为"拼接截断"，保证不丢
     */
    private String compress(List<ChatMessageDO> batch) {
        String conversation = batch.stream()
                .map(m -> "[" + m.getRole() + "] " + m.getContent())
                .collect(Collectors.joining("\n"));
        try {
            String summary = ChatClient.create(chatModel)
                    .prompt()
                    .system("你是对话压缩器。把下面的多轮对话压缩成一段简洁、保留关键事实与用户需求的中文摘要，只输出摘要本身。")
                    .user(conversation)
                    .call()
                    .content();
            if (StringUtils.hasText(summary)) {
                return summary.trim();
            }
            return fallbackSummary(conversation);
        } catch (Exception e) {
            log.warn("## 摘要压缩调用失败，降级为拼接截断", e);
            return fallbackSummary(conversation);
        }
    }

    private String fallbackSummary(String conversation) {
        return conversation.length() > 1500 ? conversation.substring(0, 1500) : conversation;
    }
}
