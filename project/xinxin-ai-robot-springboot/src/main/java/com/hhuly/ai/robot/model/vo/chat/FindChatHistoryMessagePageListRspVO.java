package com.hhuly.ai.robot.model.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindChatHistoryMessagePageListRspVO {
    /**
     * 消息 ID
     */
    private Long id;
    /**
     * 对话 ID
     */
    private String chatId;
    /**
     * 内容
     */
    private String content;
    /**
     * 消息类型
     */
    private String role;
    /**
     * 推理内容
     */
    private String reasoning;
    /**
     * 发布时间
     */
    private LocalDateTime createTime;

}
