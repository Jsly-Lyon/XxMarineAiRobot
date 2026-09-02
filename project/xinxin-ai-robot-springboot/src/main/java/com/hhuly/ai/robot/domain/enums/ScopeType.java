package com.hhuly.ai.robot.domain.enums;

/**
 * 固定槽记忆归属范围
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 对应 t_user_memory.scope；USER 本会话私有记忆，GLOBAL 系统预置的领域设定（程序员预置、不随对话写入）
 */
public enum ScopeType {

    USER("USER", "本会话私有"),
    GLOBAL("GLOBAL", "系统预置共享");

    /** 数据库存储值 */
    private final String value;
    /** 中文描述 */
    private final String desc;

    ScopeType(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }
}
