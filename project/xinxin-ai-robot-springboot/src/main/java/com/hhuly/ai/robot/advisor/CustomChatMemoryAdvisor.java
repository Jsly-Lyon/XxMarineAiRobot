package com.hhuly.ai.robot.advisor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.hhuly.ai.robot.domain.dos.ChatMessageDO;
import com.hhuly.ai.robot.domain.mapper.ChatMessageMapper;
import com.hhuly.ai.robot.model.vo.chat.AiChatReqVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
public class CustomChatMemoryAdvisor implements StreamAdvisor {

    /** 压缩摘要消息角色（超窗消息压缩后以 role=summary 写回窗口） */
    private static final String SESSION_SUMMARY_ROLE = "summary";

    private final ChatMessageMapper chatMessageMapper;
    private final AiChatReqVO aiChatReqVO;
    private final int limit;

    public CustomChatMemoryAdvisor(ChatMessageMapper chatMessageMapper, AiChatReqVO aiChatReqVO, int limit) {
        this.chatMessageMapper = chatMessageMapper;
        this.aiChatReqVO = aiChatReqVO;
        this.limit = limit;
    }

    @Override
    public int getOrder() {
        return 2; // order 值越小，越先执行
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        log.info("## 自定义聊天记忆 Advisor...");

        // 对话 UUID
        String chatUuid = aiChatReqVO.getChatId();

        // 查询数据库拉取最新的【活跃】聊天消息（压缩/归档的 archived=1 不再注入）
        List<ChatMessageDO> messages = chatMessageMapper.selectList(Wrappers.<ChatMessageDO>lambdaQuery()
                .eq(ChatMessageDO::getChatUuid, chatUuid) // 查询指定对话 UUID 下的聊天记录
                .eq(ChatMessageDO::getArchived, 0) // 仅窗口内活跃消息
                .orderByDesc(ChatMessageDO::getCreateTime) // 查询最新的消息
                .last(String.format("LIMIT %d", limit))); // 仅查询 LIMIT 条

        // 按发布时间升序排列
        List<ChatMessageDO> sortedMessages = messages.stream()
                .sorted(Comparator.comparing(ChatMessageDO::getCreateTime)) // 升序排列
                .toList();

        // 所有消息
        List<Message> messageList = Lists.newArrayList();

        // 先放"本会话此前摘要"（超窗压缩回写窗口的 role=summary 消息），作为全局前置
        for (ChatMessageDO chatMessageDO : sortedMessages) {
            if (SESSION_SUMMARY_ROLE.equals(chatMessageDO.getRole())) {
                messageList.add(new SystemMessage("【本会话此前摘要】\n" + chatMessageDO.getContent()));
            }
        }

        // 再按时间升序追加窗口内的用户/AI 消息
        for (ChatMessageDO chatMessageDO : sortedMessages) {
            // 消息类型
            String type  = chatMessageDO.getRole();
            if (SESSION_SUMMARY_ROLE.equals(type)) { // 摘要已放最前，这里跳过
                continue;
            }
            if (Objects.equals(type, MessageType.USER.getValue())) { // 用户消息
                Message userMessage = new UserMessage(chatMessageDO.getContent());
                messageList.add(userMessage);
            } else if (Objects.equals(type, MessageType.ASSISTANT.getValue())) { // AI 助手消息
                Message assistantMessage = new AssistantMessage(chatMessageDO.getContent());
                messageList.add(assistantMessage);
            }
        }

        // 除了记忆消息，还需要添加当前用户消息
        messageList.addAll(chatClientRequest.prompt().getInstructions());

        // 构建一个新的 ChatClientRequest 请求对象
        ChatClientRequest processedChatClientRequest = chatClientRequest
                .mutate()
                .prompt(chatClientRequest.prompt().mutate().messages(messageList).build())
                .build();

        return streamAdvisorChain.nextStream(processedChatClientRequest);
    }
}

