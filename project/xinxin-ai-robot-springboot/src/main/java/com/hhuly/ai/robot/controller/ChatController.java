package com.hhuly.ai.robot.controller;

import com.google.common.collect.Lists;
import com.hhuly.ai.robot.advisor.CustomChatMemoryAdvisor;
import com.hhuly.ai.robot.advisor.CustomStreamLoggerAndMessage2DBAdvisor;
import com.hhuly.ai.robot.advisor.NetworkSearchAdvisor;
import com.hhuly.ai.robot.advisor.StructuredMemoryInjectAdvisor;
import com.hhuly.ai.robot.aspect.ApiOperationLog;
import com.hhuly.ai.robot.domain.mapper.ChatMessageMapper;
import com.hhuly.ai.robot.domain.mapper.ChatWorkingMemoryMapper;
import com.hhuly.ai.robot.domain.mapper.UserMemoryMapper;
import com.hhuly.ai.robot.event.ChatWindowRollEvent;
import com.hhuly.ai.robot.model.vo.chat.AiChatReqVO;
import com.hhuly.ai.robot.model.vo.chat.AiResponse;
import com.hhuly.ai.robot.model.vo.chat.DeleteChatReqVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryMessagePageListReqVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryMessagePageListRspVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryPageListReqVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryPageListRspVO;
import com.hhuly.ai.robot.model.vo.chat.NewChatReqVO;
import com.hhuly.ai.robot.model.vo.chat.RenameChatReqVO;
import com.hhuly.ai.robot.service.ChatService;
import com.hhuly.ai.robot.service.MemoryExtractionService;
import com.hhuly.ai.robot.service.SearXNGService;
import com.hhuly.ai.robot.service.SearchResultContentFetcherService;
import com.hhuly.ai.robot.utils.MemoryBlockStripper;
import com.hhuly.ai.robot.utils.PageResponse;
import com.hhuly.ai.robot.utils.Response;
import com.hhuly.ai.robot.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: li
 * @Date: 2026/9/1 8:42
 * @Version: v1.0.0
 * @Description: 对话
 **/
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private ApplicationEventPublisher eventPublisher;
    @Resource
    private OpenAiChatModel openAiChatModel;
    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private ChatWorkingMemoryMapper chatWorkingMemoryMapper;
    @Resource
    private UserMemoryMapper userMemoryMapper;
    @Resource
    private MemoryExtractionService memoryExtractionService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SearXNGService searXNGService;
    @Resource
    private SearchResultContentFetcherService searchResultContentFetcherService;


    /** 调试用户 ID（登录体系就绪前占位，后续改为从 Sa-Token 上下文解析注入） */
    private static final String DEV_USER_ID = "guest";

    @PostMapping("/new")
    @ApiOperationLog(description = "新建对话")
    public Response<?> newChat(@RequestBody @Validated NewChatReqVO newChatReqVO) {
        return chatService.newChat(newChatReqVO);
    }

    /**
     * 流式对话
     * @return
     */
    @PostMapping(value = "/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "流式对话")
    public Flux<AiResponse> chat(@RequestBody @Validated AiChatReqVO aiChatReqVO) {
        // 用户消息
        String userMessage = aiChatReqVO.getMessage();
        // 模型名称
        String modelName = aiChatReqVO.getModelName();
        // 温度值
        Double temperature = aiChatReqVO.getTemperature();
        // 是否开启联网搜索
        boolean networkSearch = aiChatReqVO.getNetworkSearch();

        // 动态设置调用的模型名称、温度值
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = ChatClient.create(openAiChatModel)
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .model(modelName) // 模型名称
                        .temperature(temperature)) // 温度值
                .user(userMessage); // 用户提示词

        // Advisor 集合
        List<Advisor> advisors = Lists.newArrayList();

        if (networkSearch) {
            advisors.add(new NetworkSearchAdvisor(searXNGService, searchResultContentFetcherService));
        } else {
            // 短期消息窗口 Advisor（最近 50 条消息作为记忆，order=2）
            advisors.add(new CustomChatMemoryAdvisor(chatMessageMapper, aiChatReqVO, 50));
            // 结构化工作记忆注入 Advisor（本会话固定槽 + 工作区 + GLOBAL 领域设定，order=3）
            advisors.add(new StructuredMemoryInjectAdvisor(chatWorkingMemoryMapper, userMemoryMapper, aiChatReqVO));
        }

        // 流式日志 + 消息落库 Advisor（order=99）
        advisors.add(new CustomStreamLoggerAndMessage2DBAdvisor(chatMessageMapper, aiChatReqVO, transactionTemplate));
        // 应用 Advisor 集合
        chatClientRequestSpec.advisors(advisors);

        // 对话 UUID
        String chatUuid = aiChatReqVO.getChatId();
        // 数据隔离：仅会话归属者可发起流式对话，他人 uuid 一律视为不存在
        chatService.assertChatOwner(chatUuid);
        // 记忆 JSON 容器：正文剥离工具把模型回复开头的记忆块吞掉，JSON 写入此容器，流结束时落库
        AtomicReference<String> memoryJson = new AtomicReference<>();
        // 正文记忆块剥离状态机（逐帧喂入，吞掉开头的 [MEMORY_START]...[/MEMORY_END] 记忆块）
        MemoryBlockStripper.FrameStripper bodyStripper = new MemoryBlockStripper.FrameStripper(memoryJson);
        // 推理内容游标：reasoningContent 是「累积值」，记录已下发长度用于截取本帧增量
        int[] reasoningLen = {0};

        // 流式输出：推理增量独立下发为 reasoning 帧，正式回答正文经记忆块剥离后下发为 v 帧
        Flux<AiResponse> aiResponseFrames = chatClientRequestSpec
                .stream()
                .chatResponse()
                .<AiResponse>handle((chatResponse, sink) -> {
                    // getResult() 为 null（usage/空帧等）直接跳过
                    if (chatResponse == null || chatResponse.getResult() == null) {
                        return;
                    }
                    AssistantMessage message = chatResponse.getResult().getOutput();

                    // 1. 推理内容（累积值）：仅本帧有新增时下发增量（思考过程）
                    String reasoningContent = extractReasoningContent(message);
                    if (reasoningContent.length() > reasoningLen[0]) {
                        sink.next(AiResponse.builder()
                                .reasoning(reasoningContent.substring(reasoningLen[0]))
                                .build());
                        reasoningLen[0] = reasoningContent.length();
                    }

                    // 2. 正式回答正文：经记忆块剥离后下发
                    String text = message.getText();
                    if (text != null && !text.isEmpty()) {
                        for (String body : bodyStripper.feed(text)) {
                            sink.next(AiResponse.builder().v(body).build());
                        }
                    }
                })
                .doOnComplete(() -> {
                    String json = memoryJson.get();
                    if (json == null || json.isBlank()) {
                        log.info("## 本次回复未剥离到记忆块（memoryJson 为空），跳过记忆落库");
                    } else {
                        log.info("## 已剥离到记忆块，开始落库，json 长度 = {}", json.length());
                        try {
                            memoryExtractionService.storeFromJson(chatUuid, DEV_USER_ID, json);
                        } catch (Exception ex) {
                            log.error("## 记忆落库异常", ex);
                        }
                    }

                    // 触发会话窗口滚动：超窗消息压缩回窗口，仍超则归档 t_session_memory（异步，不阻塞回复）
                    try {
                        eventPublisher.publishEvent(new ChatWindowRollEvent(chatUuid, UserContext.getUserId()));
                    } catch (Exception ex) {
                        log.error("## 发布会话窗口滚动事件失败", ex);
                    }
                });

        return aiResponseFrames;
    }

    /**
     * 从 AI 回复消息中提取推理内容（metadata 中的 reasoningContent，累积值）；无推理时返回空串
     *
     * @param message AI 助手消息
     * @return 推理内容（可能为空串）
     */
    private String extractReasoningContent(AssistantMessage message) {
        Object reasoning = message.getMetadata().get("reasoningContent");
        return reasoning == null ? "" : reasoning.toString();
    }

    /**
     * 查询对话历史消息
     * @param findChatHistoryMessagePageListReqVO
     * @return
     */
    @PostMapping("/message/list")
    @ApiOperationLog(description = "查询对话历史消息")
    public PageResponse<FindChatHistoryMessagePageListRspVO> findChatMessagePageList(@RequestBody @Validated FindChatHistoryMessagePageListReqVO findChatHistoryMessagePageListReqVO) {
        return chatService.findChatHistoryMessagePageList(findChatHistoryMessagePageListReqVO);
    }

    /**
     * 查询历史对话
     * @param findChatHistoryPageListReqVO
     * @return
     */
    @PostMapping("/list")
    @ApiOperationLog(description = "查询历史对话")
    public PageResponse<FindChatHistoryPageListRspVO> findChatHistoryPageList(@RequestBody @Validated FindChatHistoryPageListReqVO findChatHistoryPageListReqVO) {
        return chatService.findChatHistoryPageList(findChatHistoryPageListReqVO);
    }

    /**
     * 重命名对话摘要
     * @param renameChatReqVO
     * @return
     */
    @PostMapping("/summary/rename")
    @ApiOperationLog(description = "重命名对话摘要")
    public Response<?> renameChatSummary(@RequestBody @Validated RenameChatReqVO renameChatReqVO) {
        return chatService.renameChatSummary(renameChatReqVO);
    }

    /**
     * 删除对话
     * @param deleteChatReqVO
     * @return
     */
    @PostMapping("/delete")
    @ApiOperationLog(description = "删除对话")
    public Response<?> deleteChat(@RequestBody @Validated DeleteChatReqVO deleteChatReqVO) {
        return chatService.deleteChat(deleteChatReqVO);
    }
}
