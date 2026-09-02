package com.hhuly.mcp.qq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @Author: li
 * @Date: 2026/8/22 19:12
 * @Version: v1.0.0
 * @Description: RestTemplate 配置类
 **/
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 连接超时时间：10秒
        factory.setReadTimeout(10000); // 读取超时时间：10秒
        return new RestTemplate(factory);
    }
}
