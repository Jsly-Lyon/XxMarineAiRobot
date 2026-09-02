package com.hhuly.ai.robot.controller;

import com.alibaba.dashscope.aigc.imagegeneration.ImageGeneration;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationMessage;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationParam;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * @Author: li
 * @Date: 2026/8/26 19:26
 * @Version: v1.0.0
 * @Description: 文生图
 **/
@RestController
@RequestMapping("/v10/ai")
@Slf4j
public class Text2ImgController {

    /**
     * 阿里百炼 API Key
     */
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    /**
     * 调用阿里百炼「通义万相」文生图大模型（wan2.6-t2i），根据提示词生成图片
     */
    @GetMapping("/text2img")
    public String text2Image(@RequestParam(value = "prompt") String prompt) {

        // 构建用户消息
        //    - role: 消息角色，固定为 "user"
        //    - content: 内容数组，每个元素为一个 map，这里通过 {"text": prompt} 传入正向提示词
        ImageGenerationMessage message = ImageGenerationMessage.builder()
                .role("user")
                .content(Collections.singletonList(
                        Collections.singletonMap("text", prompt)
                )).build();

        // 构建文生图请求参数
        ImageGenerationParam param = ImageGenerationParam.builder()
                .apiKey(apiKey)                 // 百炼 API Key
                .model("wan2.6-t2i")            // 模型名称
                .n(1)                        // 生成图片的数量，这里指定生成 1 张
                .size("1280*1280")              // 输出图像分辨率，格式为 "宽*高"
                .negativePrompt("")             // 反向提示词：描述不希望出现的内容（如 "模糊、多余的手指"），为空表示不限制
                .promptExtend(true)             // 是否开启 prompt 智能改写（默认 true）：自动扩展较短 prompt 以提升出图效果，额外耗时约 3~5 秒
                .watermark(false)               // 是否给生成图片添加水印（默认 false）
                .messages(Collections.singletonList(message))   // 用户消息列表
                .build();


        // 同步调用大模型，直到模型生成完成
        ImageGeneration imageGeneration = new ImageGeneration();
        ImageGenerationResult result = null;
        try {
            log.info("## 同步调用，请稍等一会...");
            result = imageGeneration.call(param);
        } catch (ApiException | NoApiKeyException | UploadFileException e){
            log.error("", e);
        }

        // 返回生成结果
        return JsonUtils.toJson(result);
    }
}

