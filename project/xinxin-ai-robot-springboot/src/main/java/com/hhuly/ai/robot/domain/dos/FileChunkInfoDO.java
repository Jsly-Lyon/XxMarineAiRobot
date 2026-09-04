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
 * 分片信息表：记录某文件已上传成功的分片（断点续传用）
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_file_chunk_info")
public class FileChunkInfoDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 文件 MD5 值 */
    private String fileMd5;
    /** 分片序号（从 0 开始） */
    private Integer chunkNumber;
    /** 分片文件存储路径 */
    private String chunkPath;
    /** 分片大小（字节） */
    private Long chunkSize;
    private LocalDateTime createTime;
}
