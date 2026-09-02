package com.hhuly.ai.robot.model.vo.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册请求参数
 *
 * @author: li
 * @date: 2026/9/2
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserReqVO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6~64 之间")
    private String password;

    /** 昵称（可选，缺省用用户名） */
    private String nickname;
}
