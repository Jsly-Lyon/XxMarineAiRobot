package com.hhuly.ai.robot.aspect;

import com.hhuly.ai.robot.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @Date: 2026/8/30 12:45
 * @Version: v1.0.0
 * @Description: API 操作日志切面
 **/
@Aspect
@Component
@Slf4j
public class ApiOperationLogAspect {

    /** 以自定义 @ApiOperationLog 注解为切点，凡是添加 @ApiOperationLog 的方法，都会执行环绕中的代码 */
    @Pointcut("@annotation(com.hhuly.ai.robot.aspect.ApiOperationLog)")
    public void apiOperationLog() {}

    /**
     * 环绕
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("apiOperationLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 请求开始时间
        long startTime = System.currentTimeMillis();

        // 获取被请求的类和方法
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 请求入参
        Object[] args = joinPoint.getArgs();
        // 入参转 JSON 字符串
        String argsJsonStr = Arrays.stream(args).map(toJsonStr()).collect(Collectors.joining(", "));

        // 功能描述信息
        String description = getApiOperationLogDescription(joinPoint);

        // 打印请求相关参数
        log.info("====== 请求开始: [{}], 入参: {}, 请求类: {}, 请求方法: {} =================================== ",
                description, argsJsonStr, className, methodName);

        // 执行切点方法
        Object result = joinPoint.proceed();

        // 执行耗时
        long executionTime = System.currentTimeMillis() - startTime;

        // 打印出参等相关信息（序列化失败时降级为 toString，避免打断请求）
        log.info("====== 请求结束: [{}], 耗时: {}ms, 出参: {} =================================== ",
                description, executionTime, toResultStr(result));

        return result;
    }

    /**
     * 获取注解的描述信息
     * @param joinPoint
     * @return
     */
    private String getApiOperationLogDescription(ProceedingJoinPoint joinPoint) {
        // 1. 从 ProceedingJoinPoint 获取 MethodSignature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 2. 使用 MethodSignature 获取当前被注解的 Method
        Method method = signature.getMethod();

        // 3. 从 Method 中提取 LogExecution 注解
        ApiOperationLog apiOperationLog = method.getAnnotation(ApiOperationLog.class);

        // 4. 从 LogExecution 注解中获取 description 属性
        return apiOperationLog.description();
    }

    /**
     * 入参安全转 JSON：文件对象只记文件名/大小；不可序列化类型降级为 toString
     */
    private Function<Object, String> toJsonStr() {
        return arg -> {
            if (arg == null) {
                return "null";
            }
            if (arg instanceof MultipartFile file) {
                // MultipartFile 无法被 JSON 序列化（内部 Resource 不是 URL），仅记录关键信息
                return "MultipartFile{name=" + file.getOriginalFilename() + ", size=" + file.getSize() + "}";
            }
            try {
                return JsonUtil.toJsonString(arg);
            } catch (Exception e) {
                return String.valueOf(arg);
            }
        };
    }

    /**
     * 出参安全转 JSON：序列化失败（如返回流式 Flux 等）降级为 toString
     */
    private String toResultStr(Object result) {
        if (result == null) {
            return "null";
        }
        try {
            return JsonUtil.toJsonString(result);
        } catch (Exception e) {
            return String.valueOf(result);
        }
    }
}
