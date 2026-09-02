package com.hhuly.ai.robot.domain.enums;

/**
 * 工作区记忆区域类型
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 对应 t_chat_working_memory.area_type
 */
public enum AreaType {

    GOAL("GOAL", "当前目标/用户需求"),
    CONFIRMED_FACT("CONFIRMED_FACT", "已确认的中间结论/关键事实"),
    HYPOTHESIS("HYPOTHESIS", "待验证的假设/结论");

    /** 数据库存储值 */
    private final String value;
    /** 中文描述 */
    private final String desc;

    AreaType(String value, String desc) {
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
