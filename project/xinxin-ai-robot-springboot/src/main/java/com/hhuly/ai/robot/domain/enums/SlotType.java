package com.hhuly.ai.robot.domain.enums;

/**
 * 固定槽记忆类型
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 对应 t_user_memory.slot_type
 */
public enum SlotType {

    PREFERENCE("PREFERENCE", "用户偏好"),
    LANGUAGE("LANGUAGE", "语言偏好"),
    RULE("RULE", "规则"),
    FORBIDDEN("FORBIDDEN", "禁忌");

    /** 数据库存储值 */
    private final String value;
    /** 中文描述 */
    private final String desc;

    SlotType(String value, String desc) {
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
