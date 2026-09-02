package com.hhuly.ai.robot.model.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录返回参数
 *
 * @author: li
 * @date: 2026/9/1
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRspVO {

    /** Sa-Token token 值（前端后续请求放入请求头 Authorization: Bearer <token>） */
    private String token;
}
