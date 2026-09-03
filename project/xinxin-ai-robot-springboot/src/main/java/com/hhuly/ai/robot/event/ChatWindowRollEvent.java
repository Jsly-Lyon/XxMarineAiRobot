package com.hhuly.ai.robot.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话滑动窗口滚动事件：消息落库后触发异步“压缩 + 归档”
 *
 * @author: li
 * @date: 2026/9/3
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatWindowRollEvent {

    /** 会话 UUID */
    private String chatUuid;

    /** 归属用户 */
    private Long userId;
}
