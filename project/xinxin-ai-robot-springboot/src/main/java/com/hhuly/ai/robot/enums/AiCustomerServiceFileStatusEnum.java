package com.hhuly.ai.robot.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 客服问答文件状态
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Getter
@AllArgsConstructor
public enum AiCustomerServiceFileStatusEnum {

    UPLOADING(0, "上传中"),
    PENDING(1, "上传成功,待处理"),
    VECTORIZING(2, "向量化中"),
    COMPLETED(3, "已完成"),
    FAILED(4, "失败");

    private Integer code;
    private String description;

    /**
     * 根据 code 获取枚举
     *
     * @param code
     * @return
     */
    public static AiCustomerServiceFileStatusEnum codeOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (AiCustomerServiceFileStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
