package com.hhuly.ai.robot.model.vo.customerService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateMarkdownFileReqVO {

    @NotNull(message = "问答文件 ID 不能为空")
    private Long id;

    @NotBlank(message = "备注信息不能为空")
    private String remark;

}