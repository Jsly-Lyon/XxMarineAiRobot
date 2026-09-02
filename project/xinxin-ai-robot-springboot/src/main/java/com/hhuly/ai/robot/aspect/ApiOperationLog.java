package com.hhuly.ai.robot.aspect;

import java.lang.annotation.*;

/**
 * @Author: li
 * @Date: 2026/8/30 12:32
 * @Version: v1.0.0
 * @Description: 日志切面注解
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface ApiOperationLog {

    /**
     * API 功能描述
     *
     * @return
     */
    String description() default "";

}
