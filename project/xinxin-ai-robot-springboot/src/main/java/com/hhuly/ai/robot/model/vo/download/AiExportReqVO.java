package com.hhuly.ai.robot.model.vo.download;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 回答导出 Markdown 请求
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExportReqVO {

    /** 导出文件名（可选，缺省按时间生成） */
    private String fileName;

    /** Markdown 内容（必填） */
    @NotBlank(message = "导出内容不能为空")
    private String content;
}
