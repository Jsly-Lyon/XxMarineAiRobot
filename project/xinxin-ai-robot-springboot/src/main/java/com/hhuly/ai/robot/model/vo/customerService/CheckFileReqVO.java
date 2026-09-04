package com.hhuly.ai.robot.model.vo.customerService;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分片上传前：文件检查入参
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckFileReqVO {

    @NotBlank(message = "文件 MD5 不能为空")
    private String fileMd5;
}
