package com.hhuly.ai.robot.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @Author: li
 * @Date: 2026/8/31 21:31
 * @Version: v1.0.0
 * @Description: AI 客服 Markdown 问答文件上传事件
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiCustomerServiceMdUploadedEvent {

    /**
     * 文件表主键（用于状态回写、删除联动）
     */
    private Long fileId;

    /**
     * 存储路径
     */
    private String filePath;

    /**
     * 元数据
     */
    private Map<String, Object> metadatas;
}
