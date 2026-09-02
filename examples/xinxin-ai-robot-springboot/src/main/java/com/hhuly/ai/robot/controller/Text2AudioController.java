package com.hhuly.ai.robot.controller;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @Author: li
 * @Date: 2026/8/26 19:51
 * @Version: v1.0.0
 * @Description: 文生音频
 **/
@RestController
@RequestMapping("/v11/ai")
@Slf4j
public class Text2AudioController {


    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    /**
     * 调用阿里百炼-语音合成大模型
     * @param prompt
     * @return
     */
    @GetMapping("/text2audio")
    public String text2audio(@RequestParam(value = "prompt") String prompt) {

        // 模型
        String model = "cosyvoice-v3-flash";

        // 音色
        String voice = "longanhuan_v3";

        // 请求参数
        SpeechSynthesisParam param =
                SpeechSynthesisParam.builder()
                        .apiKey(apiKey) // Api Key
                        .model(model) // 模型
                        .voice(voice) // 音色
                        .build();

        // 同步模式：禁用回调（第二个参数为null）
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = null;
        try {
            // 阻塞直至音频返回
            audio = synthesizer.call(prompt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 任务结束关闭websocket连接
            synthesizer.getDuplexApi().close(1000, "bye");
        }

        if (audio != null) {
            // 音频文件存储路径
            String path = "E:\\result-audio.mp3";

            // 将音频数据保存到本地文件中
            File file = new File(path);

            // 首次发送文本时需建立 WebSocket 连接，因此首包延迟会包含连接建立的耗时
            log.info("[Metric] requestId为：{}, 首包延迟（毫秒）为：{}", synthesizer.getLastRequestId(), synthesizer.getFirstPackageDelay());

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(audio.array());
            } catch (IOException e) {
                log.error("", e);
            }
        }

        return "success";
    }
}
