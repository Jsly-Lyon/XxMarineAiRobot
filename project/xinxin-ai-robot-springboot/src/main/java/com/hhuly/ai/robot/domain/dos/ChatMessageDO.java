package com.hhuly.ai.robot.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: li
 * @Date: 2026/8/31 11:33
 * @Version: v1.0.0
 * @Description: 聊天消息 DO 实体类
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_chat_message")
public class ChatMessageDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String chatUuid;
    private String content;
    private String role;
    /** 0=窗口活跃 1=已压缩/归档（不再注入模型） */
    private Integer archived;
    private LocalDateTime createTime;
    private String reasoningContent;
}
