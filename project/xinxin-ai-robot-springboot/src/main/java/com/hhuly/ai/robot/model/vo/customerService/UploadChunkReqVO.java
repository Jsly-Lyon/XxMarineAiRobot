package com.hhuly.ai.robot.model.vo.customerService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件分片上传入参（multipart/form-data）
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadChunkReqVO {

    /**
     * 文件 MD5 值
     */
    private String fileMd5;

    /**
     * 原始文件名称
     */
    private String fileName;

    /**
     * 原始文件大小（字节）
     */
    private Long fileSize;

    /**
     * 当前分片序号（从 0 开始）
     */
    private Integer chunkNumber;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 分片文件
     */
    private MultipartFile chunk;
}
