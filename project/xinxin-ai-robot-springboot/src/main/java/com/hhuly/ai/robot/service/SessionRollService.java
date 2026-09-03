package com.hhuly.ai.robot.service;

/**
 * 会话滑动窗口滚动服务
 *
 * @author: li
 * @date: 2026/9/3
 * @description: 活跃消息超过窗口上限时，异步执行：最旧普通消息批量压缩成摘要放回窗口；
 * 若连摘要都超窗，则把最旧部分持久化到 t_session_memory（长期记忆，暂不向量化）。
 **/
public interface SessionRollService {

    /**
     * 执行一次窗口滚动（压缩 + 必要时归档）
     *
     * @param chatUuid 会话 UUID
     * @param userId   归属用户
     */
    void roll(String chatUuid, Long userId);
}
