package com.hhuly.ai.robot.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

import java.time.LocalDateTime;

/**
 * @Author: 犬小哈
 * @Date: 2026/8/26 14:51
 * @Version: v1.0.0
 * @Description: 日期 Tool
 **/
@Slf4j
public class DateTimeTools {

    @Tool(description = "获取当前日期和时间")
    String getCurrentDateTime() {
        return LocalDateTime.now().toString();
    }

}
