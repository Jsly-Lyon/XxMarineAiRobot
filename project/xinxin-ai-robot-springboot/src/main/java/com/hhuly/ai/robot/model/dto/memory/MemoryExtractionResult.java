package com.hhuly.ai.robot.model.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 大模型抽取出的记忆变更结果
 *
 * @author: li
 * @date: 2026/8/12
 * @description: 对话结束后由大模型按固定结构输出，程序解析后分别写入工作区表与固定槽表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryExtractionResult {

    /** 工作区变更（当前任务目标/已确认事实/待验证假设） */
    private List<WorkingAreaItem> workingArea;

    /** 固定槽变更（用户偏好/语言/规则/禁忌，含归属范围） */
    private List<UserSlotItem> slots;

    /**
     * 工作区条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkingAreaItem {

        /** 区域类型：GOAL / CONFIRMED_FACT / HYPOTHESIS */
        private String type;

        /** 记忆内容 */
        private String content;

        /** 状态：ACTIVE / CONFIRMED / REJECTED */
        private String status;
    }

    /**
     * 固定槽条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSlotItem {

        /** 槽类型：PREFERENCE / LANGUAGE / RULE / FORBIDDEN */
        private String slotType;

        /** 槽值 */
        private String content;

        /** 归属范围：USER 用户私有 / GLOBAL 全系统共享 */
        private String scope;
    }
}
