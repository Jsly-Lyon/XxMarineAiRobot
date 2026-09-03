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
 * 会话长期记忆（滚动窗口最终归档摘要）
 *
 * @author: li
 * @date: 2026/9/3
 * @description: 对应 t_session_memory；滑动窗口连“放回窗口的摘要”都放不下时，
 * 将最旧部分整段持久化到本表。同一会话按 seq 追加，续聊时按序读回恢复前情。
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_session_memory")
public class SessionMemoryDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 UUID */
    private String chatUuid;

    /** 归属用户 */
    private Long userId;

    /** 该段长期记忆文本 */
    private String memory;

    /** 同会话内追加序号 */
    private Integer seq;

    /** 覆盖的原消息范围（可空，便于追溯） */
    private Long fromMsgId;

    private Long toMsgId;

    private LocalDateTime createTime;
}
