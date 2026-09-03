package com.hhuly.ai.robot.controller;

import com.google.common.collect.Lists;
import com.hhuly.ai.robot.advisor.CustomerServiceAdvisor;
import com.hhuly.ai.robot.aspect.ApiOperationLog;
import com.hhuly.ai.robot.domain.dos.AiCustomerServiceMdStorageDO;
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
     * 问答 MD 文件上传（上传后异步向量化）
     */
    @PostMapping(value = "/md/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperationLog(description = "上传问答 Markdown 文件")
    public Response<Long> uploadMarkdownFile(@RequestPart(value = "file", required = false) MultipartFile file) {
        return customerService.uploadMarkdownFile(file);
    }

    /**
     * 问答多格式文档上传（md/txt/doc(x)/ppt(x)/pdf/html，上传后异步向量化）
     */
    @PostMapping(value = "/document/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperationLog(description = "上传问答多格式文档")
    public Response<Long> uploadDocument(@RequestPart(value = "file", required = false) MultipartFile file) {
        return customerService.uploadDocument(file);
    }

    /**
     * 删除问答文件（本地文件 + 记录 + 联动清理向量，仅本人）
     */
    @PostMapping("/md/delete")
    @ApiOperationLog(description = "删除 Markdown 问答文件")
    public Response<?> deleteMarkdownFile(@RequestBody @Validated DeleteMarkdownFileReqVO deleteMarkdownFileReqVO) {
        return customerService.deleteMarkdownFile(deleteMarkdownFileReqVO);
    }

    /**
     * 分页查询问答文件列表（仅本人）
     */
    @PostMapping("/md/list")
    @ApiOperationLog(description = "Markdown 问答文件分页查询")
    public PageResponse<FindMarkdownFilePageListRspVO> findMarkdownFilePageList(@RequestBody @Validated FindMarkdownFilePageListReqVO findMarkdownFilePageListReqVO) {
        return customerService.findMarkdownFilePageList(findMarkdownFilePageListReqVO);
    }

    /**
     * 修改问答文件信息（仅本人）
     */
    @PostMapping("/md/update")
    @ApiOperationLog(description = "修改 Markdown 问答文件信息")
    public Response<?> updateMarkdownFile(@RequestBody @Validated UpdateMarkdownFileReqVO updateMarkdownFileReqVO) {
        return customerService.updateMarkdownFile(updateMarkdownFileReqVO);
    }

    /**
     * 流式对话
     * @return
     */
    @PostMapping(value = "/chat/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "AI 智能客服对话")
    public Flux<String> chat(@RequestBody @Validated AiCustomerServiceChatReqVO aiChatReqVO) {
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

        // 流式输出
        return chatClientRequestSpec
                .stream()
                .content();
    }


}
