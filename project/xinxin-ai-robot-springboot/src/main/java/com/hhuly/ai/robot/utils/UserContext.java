package com.hhuly.ai.robot.utils;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 当前登录用户上下文
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 从 Sa-Token 会话中解析当前登录用户 ID；未登录返回 null。
 * 后续 ChatController / 记忆归属统一从这里取真实 userId。
 **/
public final class UserContext {

    private UserContext() {
    }

    /**
     * 当前登录用户 ID；未登录返回 null
     */
    public static Long getUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 当前登录用户 ID 的字符串形式（用于记忆/会话归属等字符串列）
     */
    public static String getUserIdStr() {
        Long userId = getUserId();
        return userId == null ? null : String.valueOf(userId);
    }

    /**
     * 是否已登录
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }
}
