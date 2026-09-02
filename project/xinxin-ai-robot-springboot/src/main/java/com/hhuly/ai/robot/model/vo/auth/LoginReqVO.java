package com.hhuly.ai.robot.model.vo.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求参数
 *
 * @author: LiYang
 * @date: 2026/7/22
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginReqVO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
