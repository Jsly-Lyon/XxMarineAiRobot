package com.hhuly.ai.robot.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 下载记录表（上传文件 / AI 导出文件下载均记录）
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_download_record")
public class DownloadRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 归属用户（数据隔离） */
    private Long userId;
    /** 来源类型：UPLOAD-上传文件 / AI_EXPORT-AI 导出 */
    private String sourceType;
    /** 下载文件名 */
    private String fileName;
    /** 服务端文件路径 */
    private String filePath;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 关联来源 id（上传文件的 file_storage.id / 或导出记录自身） */
    private Long sourceId;
    private LocalDateTime createTime;
}
