package com.hhuly.ai.robot.domain.enums;

/**
 * 工作区记忆状态
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 对应 t_chat_working_memory.status；假设随对话推进可在 ACTIVE -> CONFIRMED / REJECTED 之间流转
 */
public enum MemoryStatus {

    ACTIVE("ACTIVE", "在用/待验证"),
    CONFIRMED("CONFIRMED", "已验证"),
    REJECTED("REJECTED", "证伪"),
    ARCHIVED("ARCHIVED", "已归档（不再注入，保留历史）");

    /** 数据库存储值 */
    private final String value;
    /** 中文描述 */
    private final String desc;

    MemoryStatus(String value, String desc) {
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
