package com.hhuly.ai.robot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Web MVC 异步执行器配置
 *
 * @author: li
 * @date: 2026/9/3
 * @description: 为 SSE / 异步返回（Flux）配置专用线程池，替换默认不适合生产的 SimpleAsyncTaskExecutor。
 **/
@Configuration
public class WebMvcAsyncConfig implements WebMvcConfigurer {

    @Bean("mvcTaskExecutor")
    public ThreadPoolTaskExecutor mvcTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("mvc-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 使用自定义线程池处理异步请求（SSE 流式返回等）
        configurer.setTaskExecutor(mvcTaskExecutor());
        configurer.setDefaultTimeout(60_000); // 默认 60s 超时
    }
}
