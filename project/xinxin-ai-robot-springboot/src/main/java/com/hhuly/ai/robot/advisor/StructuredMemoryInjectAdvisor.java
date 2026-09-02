package com.hhuly.ai.robot.advisor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hhuly.ai.robot.domain.dos.ChatWorkingMemoryDO;
import com.hhuly.ai.robot.domain.dos.UserMemoryDO;
import com.hhuly.ai.robot.domain.enums.AreaType;
import com.hhuly.ai.robot.domain.enums.MemoryStatus;
import com.hhuly.ai.robot.domain.enums.ScopeType;
import com.hhuly.ai.robot.domain.enums.SlotType;
import com.hhuly.ai.robot.domain.mapper.ChatWorkingMemoryMapper;
import com.hhuly.ai.robot.domain.mapper.UserMemoryMapper;
import com.hhuly.ai.robot.model.vo.chat.AiChatReqVO;
import com.hhuly.ai.robot.utils.MemoryBlockStripper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构化工作记忆注入 Advisor
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 请求前读取当前对话的工作区记忆 + 当前用户（含全系统共享）的固定槽记忆，
 * 拼成一段结构化上下文块，通过 augmentSystemMessage 追加到系统提示词中。
 * 与短期消息窗口（CustomChatMemoryAdvisor 负责 messages）互不冲突，两者叠加构成记忆层。
 **/
@Slf4j
public class StructuredMemoryInjectAdvisor implements StreamAdvisor {

    /**
     * 记忆输出策略：要求主对话模型在回复开头输出结构化记忆 JSON（由 MemoryBlockStripper 吞掉并落库）
     */
    private static final String MEMORY_POLICY = """
            你拥有"对话内记忆"能力。记忆分两类，触发优先级不同：
            - workingArea（工作区）：当前正在推进的任务状态——当前目标、已确认的中间结论、待验证的假设。只要这段对话在推进一个任务（开启任务、进度变化、得出/推翻结论、提出待验证假设），本轮就必须在 workingArea 输出【该任务最新完整状态】；若本场没有任务则数组为空。
            - slots（固定槽）：仅当用户【明确表达要记住】的偏好/规则时才写，其余一律不写。

            格式严格如下，无任何额外文字：

            %s
            {"workingArea":[{"type":"GOAL","content":"当前目标","status":"ACTIVE"},{"type":"CONFIRMED_FACT","content":"本场已确认的关键事实","status":"CONFIRMED"},{"type":"HYPOTHESIS","content":"待验证假设","status":"ACTIVE"}],"slots":[{"slotType":"PREFERENCE","content":"用户明确要求记住的偏好","scope":"USER"},{"slotType":"LANGUAGE","content":"用户语言偏好","scope":"GLOBAL"},{"slotType":"RULE","content":"用户明确要求的规则","scope":"GLOBAL"},{"slotType":"FORBIDDEN","content":"用户明确要求的禁忌","scope":"USER"}]}
            %s

            规则：
            1. 任务型对话（有目标/进度/结论变化）本轮【必须】输出记忆块并更新 workingArea，即使 slots 为空；仅当既无任务状态变化、用户也未声明偏好时，才不输出记忆块。
            2. type 仅限 GOAL/CONFIRMED_FACT/HYPOTHESIS；slotType 仅限 PREFERENCE/LANGUAGE/RULE/FORBIDDEN。
            3. slots 全部归属当前会话，scope 一律填 USER（本会话私有）。不要输出 GLOBAL：GLOBAL 领域设定由系统预置维护，不需要模型生成。
            4. 块内必须是合法 JSON，禁止注释与多余文字。
            5. 记忆块必须是回复的第一个字符，正文紧跟结束标记之后。
            6. slots（固定槽）只收录用户【明确表达要记住】的信息，如"记住我叫小明""以后都用简洁回答""不要用emoji"；若用户只是闲聊随口提到、未明确要求记住，该信息最多放入 workingArea，绝不放入 slots。
            7. 不要记录模型自我介绍、通用常识、模型能力描述。
            8. RULE 与 FORBIDDEN 只记录用户本人明确表达的规则/禁忌，不要写入"模型具备的功能/安全/隐私能力"。
            9. 无法确定是否值得长期记住时，宁可省略，绝不编造或夸大。
            10. 若某条记忆已存在于你收到的【用户固定记忆】或【当前任务工作区】中，本轮不要重复输出同义条目；仅当真正出现新的信息时才输出记忆块。
            11. 当用户只是在【询问/确认】自己的偏好或记忆内容（如"我喜欢什么风格""你记得我什么""记住我什么了吗"）时，这不是新信息，不要输出记忆块，直接如实回答即可。
            """.formatted(MemoryBlockStripper.MEMORY_START, MemoryBlockStripper.MEMORY_END);

    private final ChatWorkingMemoryMapper chatWorkingMemoryMapper;
    private final UserMemoryMapper userMemoryMapper;
    private final AiChatReqVO aiChatReqVO;

    public StructuredMemoryInjectAdvisor(ChatWorkingMemoryMapper chatWorkingMemoryMapper,
                                         UserMemoryMapper userMemoryMapper,
                                         AiChatReqVO aiChatReqVO) {
        this.chatWorkingMemoryMapper = chatWorkingMemoryMapper;
        this.userMemoryMapper = userMemoryMapper;
        this.aiChatReqVO = aiChatReqVO;
    }

    @Override
    public int getOrder() {
        return 3; // 必须在 CustomChatMemoryAdvisor(order=2) 重建 messages 之后执行，把记忆 SystemMessage 插到最前，避免被重建打乱
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        // 拼接结构化记忆上下文块（本会话固定槽 + 工作区 + GLOBAL 领域设定）
        String memoryBlock = buildMemoryBlock(aiChatReqVO.getChatId());
        if (memoryBlock != null && !memoryBlock.isBlank()) {
            log.info("## 注入结构化工作记忆:\n{}", memoryBlock);
        }

        // 记忆内容 + 记忆输出策略，合成一条 SystemMessage 插到消息列表最前。
        // 本 Advisor order=3，在 CustomChatMemoryAdvisor(order=2) 重建 messages 之后执行，
        // 此时 messages 已是"历史 + 当前用户"，SystemMessage 不会被后续重建打乱顺序。
        String content = MEMORY_POLICY;
        if (memoryBlock != null && !memoryBlock.isBlank()) {
            content = memoryBlock + "\n\n" + content;
        }

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(content));
        messages.addAll(chatClientRequest.prompt().getInstructions());

        Prompt newPrompt = chatClientRequest.prompt().mutate().messages(messages).build();
        ChatClientRequest processedRequest = chatClientRequest.mutate().prompt(newPrompt).build();
        return streamAdvisorChain.nextStream(processedRequest);
    }

    /**
     * 查询并拼接结构化记忆块：先固定槽，再工作区
     */
    private String buildMemoryBlock(String chatUuid) {
        StringBuilder sb = new StringBuilder();

        // 1. 固定槽：本会话私有的用户偏好/规则 + 系统预置的 GLOBAL 领域设定
        List<UserMemoryDO> slots = userMemoryMapper.selectList(Wrappers.<UserMemoryDO>lambdaQuery()
                .eq(UserMemoryDO::getIsActive, true)
                .and(w -> w.eq(UserMemoryDO::getScope, ScopeType.USER.getValue())
                        .eq(UserMemoryDO::getChatUuid, chatUuid))
                .or(w -> w.eq(UserMemoryDO::getScope, ScopeType.GLOBAL.getValue())));

        if (!CollectionUtils.isEmpty(slots)) {
            sb.append("【固定记忆】（本会话用户偏好/规则 + 系统领域设定，必须始终遵守，不要违背）\n");
            for (UserMemoryDO slot : slots) {
                sb.append("- ").append(areaDesc(SlotType.class, slot.getSlotType()))
                        .append("：").append(slot.getContent()).append("\n");
            }
        }

        // 2. 工作区：当前对话的目标/事实/假设
        List<ChatWorkingMemoryDO> workingMemories = chatWorkingMemoryMapper.selectList(
                Wrappers.<ChatWorkingMemoryDO>lambdaQuery()
                        .eq(ChatWorkingMemoryDO::getChatUuid, chatUuid)
                        .in(ChatWorkingMemoryDO::getStatus,
                                MemoryStatus.ACTIVE.getValue(),
                                MemoryStatus.CONFIRMED.getValue())
                        .orderByAsc(ChatWorkingMemoryDO::getId));

        if (!CollectionUtils.isEmpty(workingMemories)) {
            sb.append("【当前任务工作区】（围绕目标推进，不要偏离；已确认事实直接采用，待验证假设不要当成事实）\n");
            for (ChatWorkingMemoryDO memory : workingMemories) {
                String tag = areaDesc(AreaType.class, memory.getAreaType());
                String statusTag = MemoryStatus.CONFIRMED.getValue().equals(memory.getStatus()) ? "（已验证）" : "";
                sb.append("- ").append(tag).append(statusTag).append("：").append(memory.getContent()).append("\n");
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }

    /**
     * 根据枚举 value 取中文描述
     */
    private String areaDesc(Class<?> enumType, String value) {
        if (enumType == AreaType.class) {
            for (AreaType item : AreaType.values()) {
                if (item.getValue().equals(value)) {
                    return item.getDesc();
                }
            }
        } else if (enumType == SlotType.class) {
            for (SlotType item : SlotType.values()) {
                if (item.getValue().equals(value)) {
                    return item.getDesc();
                }
            }
        }
        return value;
    }
}
