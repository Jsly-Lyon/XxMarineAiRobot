package com.hhuly.ai.robot.model.vo.customerService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分片上传前：文件检查出参
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckFileRspVO {

    /**
     * 文件记录是否存在
     */
    private Boolean exists;

    /**
     * 是否需要（继续）上传分片
     */
    private Boolean needUpload;

    /**
     * 已上传成功的分片序号（断点续传；非空时前端只补传缺失分片）
     */
    private List<Integer> uploadedChunks;
}
