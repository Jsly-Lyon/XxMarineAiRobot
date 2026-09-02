package com.hhuly.ai.robot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hhuly.ai.robot.domain.dos.ChatWorkingMemoryDO;
import com.hhuly.ai.robot.domain.dos.UserMemoryDO;
import com.hhuly.ai.robot.domain.enums.AreaType;
import com.hhuly.ai.robot.domain.enums.MemoryStatus;
import com.hhuly.ai.robot.domain.enums.ScopeType;
import com.hhuly.ai.robot.domain.mapper.ChatWorkingMemoryMapper;
import com.hhuly.ai.robot.domain.mapper.UserMemoryMapper;
import com.hhuly.ai.robot.model.dto.memory.MemoryExtractionResult;
import com.hhuly.ai.robot.service.MemoryExtractionService;
import com.hhuly.ai.robot.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记忆服务实现
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 记忆按会话（chat_uuid）隔离——固定槽与工作区都只属于本会话，同一用户不同会话互不可见。
 * 对话沉淀一律写入 scope=USER（绑定 chat_uuid）；GLOBAL 层为系统预置的领域设定，不随对话写入。
 **/
@Slf4j
@Service
public class MemoryExtractionServiceImpl implements MemoryExtractionService {

    /** 同一会话最多保留的活跃已确认事实数量，超出部分自动归档（避免工作区无上限增长） */
    private static final int MAX_ACTIVE_FACT = 12;

    private final ChatWorkingMemoryMapper chatWorkingMemoryMapper;
    private final UserMemoryMapper userMemoryMapper;

    public MemoryExtractionServiceImpl(ChatWorkingMemoryMapper chatWorkingMemoryMapper,
                                       UserMemoryMapper userMemoryMapper) {
        this.chatWorkingMemoryMapper = chatWorkingMemoryMapper;
        this.userMemoryMapper = userMemoryMapper;
    }

    @Override
    public void storeFromJson(String chatUuid, String userId, String memoryJson) {
        if (!StringUtils.hasText(chatUuid) || !StringUtils.hasText(memoryJson)) {
            return;
        }

        // 去掉可能的标记包裹
        String cleaned = memoryJson.trim()
                .replaceFirst("^\\[MEMORY_START\\]\\s*", "")
                .replaceFirst("\\s*\\[MEMORY_END\\]$", "");

        try {
            MemoryExtractionResult result = JsonUtil.parseObject(cleaned, MemoryExtractionResult.class);
            if (result == null) {
                log.warn("## 记忆 JSON 解析为空，跳过落库");
                return;
            }

            // 1. 工作区：一场对话结束后重写其最终状态
            saveWorkingArea(chatUuid, result.getWorkingArea());
            // 2. 固定槽：写入本会话私有槽（去重/覆盖）
            saveChatSlots(userId, chatUuid, result.getSlots());
        } catch (Exception ex) {
            log.error("## 记忆 JSON 解析失败: {}", cleaned, ex);
        }
    }

    @Override
    public void deleteChatMemory(String chatUuid, String slotType, String content) {
        userMemoryMapper.delete(Wrappers.<UserMemoryDO>lambdaQuery()
                .eq(UserMemoryDO::getScope, ScopeType.USER.getValue())
                .eq(UserMemoryDO::getChatUuid, chatUuid)
                .eq(UserMemoryDO::getSlotType, slotType)
                .eq(UserMemoryDO::getContent, content));
        log.info("## 已删除会话固定槽记忆，chatUuid = {}, slotType = {}, content = {}", chatUuid, slotType, content);
    }

    @Override
    public void clearChatMemories(String chatUuid) {
        int deleted = userMemoryMapper.delete(Wrappers.<UserMemoryDO>lambdaQuery()
                .eq(UserMemoryDO::getScope, ScopeType.USER.getValue())
                .eq(UserMemoryDO::getChatUuid, chatUuid));
        log.info("## 已清空会话固定槽记忆 {} 条，chatUuid = {}", deleted, chatUuid);
    }

    private void saveWorkingArea(String chatUuid, List<MemoryExtractionResult.WorkingAreaItem> items) {
        // 本轮有工作区更新才逐条处理；无更新时不做删除（保留已有任务状态）
        if (!CollectionUtils.isEmpty(items)) {
            for (MemoryExtractionResult.WorkingAreaItem item : items) {
                String areaType = item.getType();
                String content = item.getContent();
                if (!StringUtils.hasText(areaType) || !StringUtils.hasText(content)) {
                    continue;
                }

                // 该会话该类型下的有效记录（REJECTED/ARCHIVED 不注入，不参与去重）
                List<ChatWorkingMemoryDO> exists = chatWorkingMemoryMapper.selectList(
                        Wrappers.<ChatWorkingMemoryDO>lambdaQuery()
                                .eq(ChatWorkingMemoryDO::getChatUuid, chatUuid)
                                .eq(ChatWorkingMemoryDO::getAreaType, areaType)
                                .notIn(ChatWorkingMemoryDO::getStatus,
                                        MemoryStatus.REJECTED.getValue(),
                                        MemoryStatus.ARCHIVED.getValue()));

                // 同类型下已有相同内容 -> 跳过（重复输出不累积）
                boolean duplicate = exists.stream()
                        .anyMatch(m -> content.equals(m.getContent()));
                if (duplicate) {
                    continue;
                }

                // GOAL 已变化 -> 旧 GOAL 归档（保留历史）再写新的，保持"最新唯一目标"
                if (AreaType.GOAL.getValue().equals(areaType) && !exists.isEmpty()) {
                    for (ChatWorkingMemoryDO old : exists) {
                        archiveRecord(old);
                    }
                }

                LocalDateTime now = LocalDateTime.now();
                chatWorkingMemoryMapper.insert(ChatWorkingMemoryDO.builder()
                        .chatUuid(chatUuid)
                        .areaType(areaType)
                        .content(content)
                        .status(defaultStatus(areaType, item.getStatus()))
                        .createTime(now)
                        .updateTime(now)
                        .build());
            }
            log.info("## 已更新工作区记忆 {} 条，chatUuid = {}", items.size(), chatUuid);
        }

        // 落库后：超量活跃事实自动归档，控制注入量不再无上限增长
        archiveExcessFacts(chatUuid);
    }

    /**
     * 归档该会话超量的活跃已确认事实：仅保留最新的 MAX_ACTIVE_FACT 条，其余置 ARCHIVED（保留历史但不注入）
     */
    private void archiveExcessFacts(String chatUuid) {
        List<ChatWorkingMemoryDO> facts = chatWorkingMemoryMapper.selectList(
                Wrappers.<ChatWorkingMemoryDO>lambdaQuery()
                        .eq(ChatWorkingMemoryDO::getChatUuid, chatUuid)
                        .eq(ChatWorkingMemoryDO::getAreaType, AreaType.CONFIRMED_FACT.getValue())
                        .in(ChatWorkingMemoryDO::getStatus,
                                MemoryStatus.ACTIVE.getValue(),
                                MemoryStatus.CONFIRMED.getValue())
                        .orderByAsc(ChatWorkingMemoryDO::getId));

        if (facts.size() <= MAX_ACTIVE_FACT) {
            return;
        }
        int excess = facts.size() - MAX_ACTIVE_FACT;
        for (int i = 0; i < excess; i++) {
            archiveRecord(facts.get(i));
        }
        log.info("## 已自动归档 {} 条旧事实，chatUuid = {}", excess, chatUuid);
    }

    /**
     * 将一条工作区记录置为 ARCHIVED（不再注入，保留历史可查）
     */
    private void archiveRecord(ChatWorkingMemoryDO record) {
        record.setStatus(MemoryStatus.ARCHIVED.getValue());
        record.setUpdateTime(LocalDateTime.now());
        chatWorkingMemoryMapper.updateById(record);
    }

    /**
     * 固定槽写入：一律落为 scope=USER 的本会话私有记忆（绑定 chat_uuid），不写 GLOBAL
     */
    private void saveChatSlots(String userId, String chatUuid, List<MemoryExtractionResult.UserSlotItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }

        for (MemoryExtractionResult.UserSlotItem item : items) {
            String slotType = item.getSlotType();
            String content = item.getContent();
            if (!StringUtils.hasText(slotType) || !StringUtils.hasText(content)) {
                continue;
            }

            // 查本会话该槽位的当前有效记录
            List<UserMemoryDO> exists = userMemoryMapper.selectList(Wrappers.<UserMemoryDO>lambdaQuery()
                    .eq(UserMemoryDO::getIsActive, true)
                    .eq(UserMemoryDO::getScope, ScopeType.USER.getValue())
                    .eq(UserMemoryDO::getChatUuid, chatUuid)
                    .eq(UserMemoryDO::getSlotType, slotType));

            // 已有完全相同或高度相似（措辞变体）内容的有效记录 -> 跳过（重申同一条时不重复写入、不触发覆盖）
            boolean duplicate = exists.stream()
                    .anyMatch(m -> content.equals(m.getContent()) || similarText(content, m.getContent()));
            if (duplicate) {
                continue;
            }

            // 同类型已存在不同的有效槽值 -> 视为用户改口：先置旧记录失效，再写入新值（避免本会话矛盾记忆并存）
            if (!exists.isEmpty()) {
                for (UserMemoryDO old : exists) {
                    old.setIsActive(false);
                    old.setUpdateTime(LocalDateTime.now());
                    userMemoryMapper.updateById(old);
                }
            }

            LocalDateTime now = LocalDateTime.now();
            userMemoryMapper.insert(UserMemoryDO.builder()
                    .scope(ScopeType.USER.getValue())
                    .userId(userId)
                    .chatUuid(chatUuid)
                    .slotType(slotType)
                    .content(content)
                    .isActive(true)
                    .sourceChatUuid(chatUuid)
                    .createTime(now)
                    .updateTime(now)
                    .build());
        }
        log.info("## 已沉淀本会话固定槽记忆 {} 条，chatUuid = {}", items.size(), chatUuid);
    }

    /**
     * 工作区默认状态：已确认事实落 CONFIRMED，其余 ACTIVE
     */
    private String defaultStatus(String type, String status) {
        if (StringUtils.hasText(status)) {
            return status;
        }
        if (AreaType.CONFIRMED_FACT.getValue().equals(type)) {
            return MemoryStatus.CONFIRMED.getValue();
        }
        return MemoryStatus.ACTIVE.getValue();
    }

    /**
     * 简单文本相似判定：字符级编辑距离不超过 2 视为同义，
     * 用于抑制模型重申同一条偏好时措辞漂移导致的反复覆盖
     */
    private boolean similarText(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        int n = a.length();
        int m = b.length();
        if (Math.abs(n - m) > 3) {
            return false;
        }
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[n][m] <= 2;
    }
}
