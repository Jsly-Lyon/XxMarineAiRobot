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
 * AI 客服问答文件存储（分片上传：登记 + 状态机，file_md5 用于秒传/断点续传）
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ai_customer_service_file_storage")
public class AiCustomerServiceFileStorageDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 原始文件名 */
    private String fileName;
    /** 文件 MD5 值，用于秒传与断点续传 */
    private String fileMd5;
    /** 完整文件存储路径（合并完成后的最终文件） */
    private String filePath;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 总分片数 */
    private Integer totalChunks;
    /** 已上传分片数 */
    private Integer uploadedChunks;
    /** 处理状态：0-上传中 1-待处理 2-向量化中 3-已完成 4-失败 */
    private Integer status;
    /** 备注 */
    private String remark;
    /** 归属用户（上传者），数据隔离 */
    private Long userId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
