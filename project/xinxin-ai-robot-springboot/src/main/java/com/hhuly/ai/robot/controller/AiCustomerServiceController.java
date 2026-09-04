package com.hhuly.ai.robot.controller;

import com.google.common.collect.Lists;
import com.hhuly.ai.robot.advisor.CustomerServiceAdvisor;
import com.hhuly.ai.robot.aspect.ApiOperationLog;
import com.hhuly.ai.robot.model.vo.chat.AiResponse;
import com.hhuly.ai.robot.model.vo.customerService.*;
import com.hhuly.ai.robot.service.CustomerKnowledgeSearchService;
import com.hhuly.ai.robot.service.CustomerService;
import com.hhuly.ai.robot.utils.PageResponse;
import com.hhuly.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/customer-service")
@Slf4j
public class AiCustomerServiceController {

    @Resource
    private CustomerService customerService;

    @Resource
    private CustomerKnowledgeSearchService customerKnowledgeSearchService;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    @Value("${customer-service.model}")
    private String model;
    @Value("${customer-service.temperature}")
    private Double temperature;

    /**
     * 问答 MD 文件上传（单次整文件上传，已废弃，等待分片上传/合并接口）
     */
//    @PostMapping(value = "/md/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @ApiOperationLog(description = "上传问答 Markdown 文件")
//    public Response<Long> uploadMarkdownFile(@RequestPart(value = "file", required = false) MultipartFile file) {
//        return customerService.uploadMarkdownFile(file);
//    }
//
//    /**
//     * 问答多格式文档上传（单次整文件上传，已废弃，等待分片上传/合并接口）
//     */
//    @PostMapping(value = "/document/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @ApiOperationLog(description = "上传问答多格式文档")
//    public Response<Long> uploadDocument(@RequestPart(value = "file", required = false) MultipartFile file) {
//        return customerService.uploadDocument(file);
//    }

    /**
     * 分片上传前：检查文件是否存在（秒传 / 断点续传）
     */
    @PostMapping("/file/check")
    @ApiOperationLog(description = "检查文件是否存在")
    public Response<CheckFileRspVO> checkFile(@RequestBody @Validated CheckFileReqVO checkFileReqVO) {
        return customerService.checkFile(checkFileReqVO);
    }

    /**
     * 文件分片上传（multipart/form-data 表单方式提交）
     */
    @PostMapping("/file/upload-chunk")
//    @ApiOperationLog(description = "文件分片上传") // 入参含 MultipartFile，日志切面序列化会报错，故注释
    public Response<?> uploadChunk(@ModelAttribute UploadChunkReqVO uploadChunkReqVO) {
        return customerService.uploadChunk(uploadChunkReqVO);
    }

    /**
     * 文件分片合并（合并完成后自动触发向量化）
     */
    @PostMapping("/file/merge-chunk")
    @ApiOperationLog(description = "文件分片合并")
    public Response<?> mergeChunk(@RequestBody @Validated MergeChunkReqVO mergeChunkReqVO) {
        return customerService.mergeChunk(mergeChunkReqVO);
    }

    /**
     * 删除问答文件（本地文件 + 记录 + 联动清理向量，仅本人）
     */
    @PostMapping("/file/delete")
    @ApiOperationLog(description = "删除问答文件")
    public Response<?> deleteMarkdownFile(@RequestBody @Validated DeleteMarkdownFileReqVO deleteMarkdownFileReqVO) {
        return customerService.deleteMarkdownFile(deleteMarkdownFileReqVO);
    }

    /**
     * 分页查询问答文件列表（仅本人）
     */
    @PostMapping("/file/list")
    @ApiOperationLog(description = "问答文件分页查询")
    public PageResponse<FindMarkdownFilePageListRspVO> findMarkdownFilePageList(@RequestBody @Validated FindMarkdownFilePageListReqVO findMarkdownFilePageListReqVO) {
        return customerService.findMarkdownFilePageList(findMarkdownFilePageListReqVO);
    }

    /**
     * 修改问答文件信息（仅本人）
     */
    @PostMapping("/file/update")
    @ApiOperationLog(description = "修改问答文件信息")
    public Response<?> updateMarkdownFile(@RequestBody @Validated UpdateMarkdownFileReqVO updateMarkdownFileReqVO) {
        return customerService.updateMarkdownFile(updateMarkdownFileReqVO);
    }

    /**
     * 流式对话
     * @return
     */
    @PostMapping(value = "/chat/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "AI 智能客服对话")
    public Flux<AiResponse> chat(@RequestBody @Validated AiCustomerServiceChatReqVO aiChatReqVO) {
        // 用户消息
        String userMessage = aiChatReqVO.getMessage();

        // 构建 ChatModel
        ChatModel chatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build())
                .build();

        // 动态设置调用的模型名称、温度值
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = ChatClient.create(chatModel)
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature))
                .user(userMessage); // 用户提示词

        // Advisor 集合
        List<Advisor> advisors = Lists.newArrayList();
        advisors.add(new CustomerServiceAdvisor(customerKnowledgeSearchService)); // 双路检索增强提示词

        // 应用 Advisor 集合
        chatClientRequestSpec.advisors(advisors);

        // 流式输出（加日志以便定位：是否检索后一直无 chunk / 报错 / 直接结束）
        return chatClientRequestSpec
                .stream()
                .content()
                .mapNotNull(text -> AiResponse.builder().v(text).build()) // 构建返参 AIResponse
                .doOnNext(resp -> log.info("## 客服流式输出块: {}", resp.getV()))
                .doOnError(err -> log.error("## 客服流式输出错误: {}", err.getMessage(), err))
                .doFinally(signalType -> log.info("## 客服流结束: signal={}", signalType));
    }


}
