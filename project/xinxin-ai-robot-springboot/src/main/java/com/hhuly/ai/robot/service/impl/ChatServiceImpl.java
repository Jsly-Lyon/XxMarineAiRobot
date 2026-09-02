package com.hhuly.ai.robot.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhuly.ai.robot.domain.dos.ChatDO;
import com.hhuly.ai.robot.domain.dos.ChatMessageDO;
import com.hhuly.ai.robot.domain.dos.ChatWorkingMemoryDO;
import com.hhuly.ai.robot.domain.dos.UserMemoryDO;
import com.hhuly.ai.robot.domain.enums.ScopeType;
import com.hhuly.ai.robot.domain.mapper.ChatMapper;
import com.hhuly.ai.robot.domain.mapper.ChatMessageMapper;
import com.hhuly.ai.robot.domain.mapper.ChatWorkingMemoryMapper;
import com.hhuly.ai.robot.domain.mapper.UserMemoryMapper;
import com.hhuly.ai.robot.enums.ResponseCodeEnum;
import com.hhuly.ai.robot.exception.BizException;
import com.hhuly.ai.robot.model.vo.chat.DeleteChatReqVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryMessagePageListReqVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryMessagePageListRspVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryPageListReqVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryPageListRspVO;
import com.hhuly.ai.robot.model.vo.chat.NewChatReqVO;
import com.hhuly.ai.robot.model.vo.chat.NewChatRspVO;
import com.hhuly.ai.robot.model.vo.chat.RenameChatReqVO;
import com.hhuly.ai.robot.service.ChatService;
import com.hhuly.ai.robot.utils.PageResponse;
import com.hhuly.ai.robot.utils.Response;
import com.hhuly.ai.robot.utils.StringUtil;
import com.hhuly.ai.robot.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Resource
    private ChatMapper chatMapper;
    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private ChatWorkingMemoryMapper chatWorkingMemoryMapper;
    @Resource
    private UserMemoryMapper userMemoryMapper;

    /**
     * 新建对话
     *
     * @param newChatReqVO
     * @return
     */
    @Override
    public Response<NewChatRspVO> newChat(NewChatReqVO newChatReqVO) {
        // 归属当前登录用户（接口已由 Sa-Token 强制登录）
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResponseCodeEnum.NOT_LOGIN);
        }

        // 用户发送的消息
        String message = newChatReqVO.getMessage();

        // 生成对话 UUID
        String uuid = UUID.randomUUID().toString();
        // 截取用户发送的消息，作为对话摘要
        String summary = StringUtil.truncate(message, 20);

        // 存储对话记录到数据库中
        chatMapper.insert(ChatDO.builder()
                .summary(summary)
                .uuid(uuid)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        // 将摘要、UUID 返回给前端
        return Response.success(NewChatRspVO.builder()
                .uuid(uuid)
                .summary(summary)
                .build());
    }

    /**
     * 查询历史消息
     *
     * @param findChatHistoryMessagePageListReqVO
     * @return
     */
    @Override
    public PageResponse<FindChatHistoryMessagePageListRspVO> findChatHistoryMessagePageList(FindChatHistoryMessagePageListReqVO findChatHistoryMessagePageListReqVO) {
        // 获取当前页、以及每页需要展示的数据数量
        Long current = findChatHistoryMessagePageListReqVO.getCurrent();
        Long size = findChatHistoryMessagePageListReqVO.getSize();
        String chatId = findChatHistoryMessagePageListReqVO.getChatId();

        // 归属校验：他人 uuid 一律视作“会话不存在”，不泄露任何信息
        requireOwnedChat(chatId, currentUserId());

        // 执行分页查询
        Page<ChatMessageDO> chatMessageDOPage = chatMessageMapper.selectPageList(current, size, chatId);

        List<ChatMessageDO> chatMessageDOS = chatMessageDOPage.getRecords();
        // DO 转 VO
        List<FindChatHistoryMessagePageListRspVO> vos = null;
        if (CollUtil.isNotEmpty(chatMessageDOS)) {
            vos = chatMessageDOS.stream()
                    .map(chatMessageDO -> FindChatHistoryMessagePageListRspVO.builder() // 构建返参 VO 实体类
                            .id(chatMessageDO.getId())
                            .chatId(chatMessageDO.getChatUuid())
                            .content(chatMessageDO.getContent())
                            .role(chatMessageDO.getRole())
                            .createTime(chatMessageDO.getCreateTime())
                            .build())
                    // 升序排序
                    .sorted(Comparator.comparing(FindChatHistoryMessagePageListRspVO::getCreateTime))
                    .collect(Collectors.toList());
        }

        return PageResponse.success(chatMessageDOPage, vos);
    }

    /**
     * 查询历史对话
     *
     * @param findChatHistoryPageListReqVO
     * @return
     */
    @Override
    public PageResponse<FindChatHistoryPageListRspVO> findChatHistoryPageList(FindChatHistoryPageListReqVO findChatHistoryPageListReqVO) {
        // 获取当前页、以及每页需要展示的数据数量
        Long current = findChatHistoryPageListReqVO.getCurrent();
        Long size = findChatHistoryPageListReqVO.getSize();

        // 执行分页查询（仅返回当前登录用户的会话）
        Page<ChatDO> chatDOPage = chatMapper.selectPageList(current, size, currentUserId());

        // 获取查询结果
        List<ChatDO> chatDOS = chatDOPage.getRecords();

        // DO 转 VO
        List<FindChatHistoryPageListRspVO> vos = null;
        if (CollUtil.isNotEmpty(chatDOS)) {
            vos = chatDOS.stream()
                    .map(chatDO -> FindChatHistoryPageListRspVO.builder() // 构建返参 VO
                            .id(chatDO.getId())
                            .uuid(chatDO.getUuid())
                            .summary(chatDO.getSummary())
                            .updateTime(chatDO.getUpdateTime())
                            .build())
                    .collect(Collectors.toList());
        }

        return PageResponse.success(chatDOPage, vos);
    }

    /**
     * 重命名对话摘要
     *
     * @param renameChatReqVO
     * @return
     */
    @Override
    public Response<?> renameChatSummary(RenameChatReqVO renameChatReqVO) {
        // 对话 ID
        Long chatId = renameChatReqVO.getId();
        // 摘要
        String summary = renameChatReqVO.getSummary();

        // 归属校验：非本人会话一律视为不存在
        requireOwnedChatById(chatId, currentUserId());

        // 根据主键 ID 更新摘要
        chatMapper.updateById(ChatDO.builder()
                .id(chatId)
                .summary(summary)
                .build());

        return Response.success();
    }

    /**
     * 删除对话
     *
     * @param deleteChatReqVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteChat(DeleteChatReqVO deleteChatReqVO) {
        // 对话 UUID
        String uuid = deleteChatReqVO.getUuid();

        // 删除对话（仅能删除归属当前登录用户的会话）
        int count = chatMapper.delete(Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getUuid, uuid)
                .eq(ChatDO::getUserId, currentUserId()));

        // 如果删除操作影响的行数为 0，说明想要删除的对话不存在
        if (count == 0) {
            throw new BizException(ResponseCodeEnum.CHAT_NOT_EXISTED);
        }

        // 批量删除对话下的所有消息
        chatMessageMapper.delete(Wrappers.<ChatMessageDO>lambdaQuery()
                .eq(ChatMessageDO::getChatUuid, uuid));

        // 联动删除该会话的工作区记忆
        chatWorkingMemoryMapper.delete(Wrappers.<ChatWorkingMemoryDO>lambdaQuery()
                .eq(ChatWorkingMemoryDO::getChatUuid, uuid));

        // 联动删除该会话的私有固定槽记忆（GLOBAL 预置领域设定不随会话删除）
        userMemoryMapper.delete(Wrappers.<UserMemoryDO>lambdaQuery()
                .eq(UserMemoryDO::getScope, ScopeType.USER.getValue())
                .eq(UserMemoryDO::getChatUuid, uuid));

        return Response.success();
    }

    @Override
    public void assertChatOwner(String chatUuid) {
        requireOwnedChat(chatUuid, currentUserId());
    }

    /**
     * 当前登录用户 ID（接口已由 Sa-Token 强制登录，兜底处理未登录情况）
     */
    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResponseCodeEnum.NOT_LOGIN);
        }
        return userId;
    }

    /**
     * 按 UUID 校验会话归属：不存在或非本人，一律抛“会话不存在”，避免泄露他人 uuid 是否存在
     */
    private ChatDO requireOwnedChat(String chatUuid, Long userId) {
        ChatDO chat = chatMapper.selectOne(Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getUuid, chatUuid)
                .eq(ChatDO::getUserId, userId));
        if (chat == null) {
            throw new BizException(ResponseCodeEnum.CHAT_NOT_EXISTED);
        }
        return chat;
    }

    /**
     * 按主键校验会话归属
     */
    private ChatDO requireOwnedChatById(Long chatId, Long userId) {
        ChatDO chat = chatMapper.selectOne(Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getId, chatId)
                .eq(ChatDO::getUserId, userId));
        if (chat == null) {
            throw new BizException(ResponseCodeEnum.CHAT_NOT_EXISTED);
        }
        return chat;
    }

}
