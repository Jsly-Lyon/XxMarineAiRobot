package com.hhuly.ai.robot.model.vo.download;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 下载记录展示对象
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadRecordRspVO {

    private Long id;
    /** UPLOAD / AI_EXPORT */
    private String sourceType;
    /** 文件名 */
    private String fileName;
    /** 文件大小（字节） */
    private Long fileSize;
    private LocalDateTime createTime;
}
