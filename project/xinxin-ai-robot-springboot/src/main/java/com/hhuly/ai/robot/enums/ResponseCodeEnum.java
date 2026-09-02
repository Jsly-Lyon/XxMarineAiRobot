package com.hhuly.ai.robot.enums;

import com.hhuly.ai.robot.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("10001", "参数错误"),


    // ----------- 业务异常状态码 -----------
    // ----------- 业务异常状态码 -----------
    CHAT_NOT_EXISTED("20000", "此对话不存在"),
    USER_NOT_EXISTED("20001", "用户不存在"),
    USERNAME_OR_PASSWORD_ERROR("20002", "用户名或密码错误"),
    USERNAME_EXISTED("20003", "用户名已存在"),
    NOT_LOGIN("20004", "未登录或登录已过期"),
    ;

    // 异常码
    private String errorCode;
    // 错误信息
    private String errorMessage;

}
