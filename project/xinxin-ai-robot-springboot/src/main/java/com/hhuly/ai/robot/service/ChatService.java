package com.hhuly.ai.robot.service;

import com.hhuly.ai.robot.model.vo.chat.DeleteChatReqVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryMessagePageListReqVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryMessagePageListRspVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryPageListReqVO;
import com.hhuly.ai.robot.model.vo.chat.FindChatHistoryPageListRspVO;
import com.hhuly.ai.robot.model.vo.chat.NewChatReqVO;
import com.hhuly.ai.robot.model.vo.chat.NewChatRspVO;
import com.hhuly.ai.robot.model.vo.chat.RenameChatReqVO;
import com.hhuly.ai.robot.utils.PageResponse;
import com.hhuly.ai.robot.utils.Response;

public interface ChatService {

    /**
     * 新建对话
     * @param newChatReqVO
     * @return
     */
    Response<NewChatRspVO> newChat(NewChatReqVO newChatReqVO);

    /**
     * 查询历史消息
     * @param findChatHistoryMessagePageListReqVO
     * @return
     */
    PageResponse<FindChatHistoryMessagePageListRspVO> findChatHistoryMessagePageList(FindChatHistoryMessagePageListReqVO findChatHistoryMessagePageListReqVO);

    /**
     * 查询历史对话
     * @param findChatHistoryPageListReqVO
     * @return
     */
    PageResponse<FindChatHistoryPageListRspVO> findChatHistoryPageList(FindChatHistoryPageListReqVO findChatHistoryPageListReqVO);

    /**
     * 重命名对话摘要
     * @param renameChatReqVO
     * @return
     */
    Response<?> renameChatSummary(RenameChatReqVO renameChatReqVO);

    /**
     * 删除对话
     * @param deleteChatReqVO
     * @return
     */
    Response<?> deleteChat(DeleteChatReqVO deleteChatReqVO);

    /**
     * 校验会话归属：仅当前登录用户可访问，否则抛业务异常（不暴露会话是否存在）
     * @param chatUuid 会话 UUID
     */
    void assertChatOwner(String chatUuid);
}
