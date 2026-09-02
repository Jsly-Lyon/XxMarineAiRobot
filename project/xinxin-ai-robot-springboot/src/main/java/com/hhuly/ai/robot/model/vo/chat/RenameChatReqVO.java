package com.hhuly.ai.robot.model.vo.chat;

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
public class RenameChatReqVO {

    @NotNull(message = "对话 ID 不能为空")
    private Long id;

    @NotBlank(message = "对话摘要不能为空")
    private String summary;

}
