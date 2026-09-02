package com.hhuly.ai.robot.controller;

import com.hhuly.ai.robot.aspect.ApiOperationLog;
import com.hhuly.ai.robot.model.vo.auth.LoginReqVO;
import com.hhuly.ai.robot.model.vo.auth.LoginRspVO;
import com.hhuly.ai.robot.model.vo.auth.RegisterUserReqVO;
import com.hhuly.ai.robot.model.vo.auth.UserInfoVO;
import com.hhuly.ai.robot.service.AuthService;
import com.hhuly.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 *
 * @author: li
 * @date: 2026/9/2
 **/
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Resource
    private AuthService authService;

    /**
     * 注册
     */
    @PostMapping("/register")
    @ApiOperationLog(description = "注册")
    public Response<?> register(@Validated @RequestBody RegisterUserReqVO registerUserReqVO) {
        return authService.register(registerUserReqVO);
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    @ApiOperationLog(description = "登录")
    public Response<LoginRspVO> login(@Validated @RequestBody LoginReqVO loginReqVO) {
        return authService.login(loginReqVO);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    @ApiOperationLog(description = "登出")
    public Response<?> logout() {
        return authService.logout();
    }

    /**
     * 获取当前登录用户信息（后端先读 Redis 会话缓存，未命中查库）
     */
    @GetMapping("/info")
    @ApiOperationLog(description = "获取当前登录用户信息")
    public Response<UserInfoVO> info() {
        return authService.getLoginUserInfo();
    }
}
