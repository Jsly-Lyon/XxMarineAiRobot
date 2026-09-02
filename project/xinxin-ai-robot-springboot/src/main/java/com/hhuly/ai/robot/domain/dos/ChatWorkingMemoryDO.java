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
 * 工作区记忆 DO 实体类
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 对应 t_chat_working_memory 表；一场对话内的动态工作区（当前目标/已确认事实/待验证假设）
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_chat_working_memory")
public class ChatWorkingMemoryDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 属于哪场对话（对应 t_chat.uuid） */
    private String chatUuid;

    /** 区域类型：GOAL / CONFIRMED_FACT / HYPOTHESIS */
    private String areaType;

    /** 记忆内容 */
    private String content;

    /** 状态：ACTIVE / CONFIRMED / REJECTED */
    private String status;

    /** 溯源：由 t_chat_message 的哪条消息提炼 */
    private Long sourceMsgId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
