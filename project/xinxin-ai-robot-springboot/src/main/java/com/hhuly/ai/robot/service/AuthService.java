package com.hhuly.ai.robot.service;

import com.hhuly.ai.robot.model.vo.auth.LoginReqVO;
import com.hhuly.ai.robot.model.vo.auth.LoginRspVO;
import com.hhuly.ai.robot.model.vo.auth.RegisterUserReqVO;
import com.hhuly.ai.robot.model.vo.auth.UserInfoVO;
import com.hhuly.ai.robot.utils.Response;

/**
 * 认证服务接口
 *
 * @author: li
 * @date: 2026/9/2
 **/
public interface AuthService {

    /**
     * 注册
     */
    Response<?> register(RegisterUserReqVO registerUserReqVO);

    /**
     * 登录：校验通过后返回 token
     */
    Response<LoginRspVO> login(LoginReqVO loginReqVO);

    /**
     * 登出
     */
    Response<?> logout();

    /**
     * 获取当前登录用户信息（优先读 Redis 会话缓存）
     */
    Response<UserInfoVO> getLoginUserInfo();
}
