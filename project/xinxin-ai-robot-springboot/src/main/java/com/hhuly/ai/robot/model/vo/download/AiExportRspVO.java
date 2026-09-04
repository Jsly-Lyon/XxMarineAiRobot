package com.hhuly.ai.robot.model.vo.download;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 导出结果
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExportRspVO {

    /** 下载记录 ID（可用 /download/record/{id} 再次下载） */
    private Long recordId;
    /** 导出文件名 */
    private String fileName;
}
