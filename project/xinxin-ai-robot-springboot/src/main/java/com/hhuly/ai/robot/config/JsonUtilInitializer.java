package com.hhuly.ai.robot.config;

import com.hhuly.ai.robot.utils.JsonUtil;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * @author: li
 * @date: 2026/8/29 22:30
 * @version: v1.0.0
 * @description: 初始化 JsonUtil，让其与 Spring 容器共用同一个 ObjectMapper，
 **/
@Configuration
public class JsonUtilInitializer {

    public JsonUtilInitializer(ObjectMapper objectMapper) {
        JsonUtil.init(objectMapper);
    }

}