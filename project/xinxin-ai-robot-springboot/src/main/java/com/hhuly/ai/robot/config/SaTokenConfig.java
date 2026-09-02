package com.hhuly.ai.robot.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器配置
 *
 * @author: li
 * @date: 2026/9/1
 * @description: 拦截除登录/注册之外的请求进行登录校验。接口级数据隔离由各 Service 依据当前登录
 * userId 过滤（会话归属），确保他人 uuid 无法访问。
 **/
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle ->
                        SaRouter.match("/**")
                                .notMatch("/auth/login", "/auth/register")
                                .check(r -> StpUtil.checkLogin())))
                .addPathPatterns("/**")
                // 用路径排除 /error：error 转发时 SaTokenContext 尚未初始化，直接跳过该请求避免二次异常
                .excludePathPatterns("/error");
    }
}
