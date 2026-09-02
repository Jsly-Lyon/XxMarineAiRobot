package com.hhuly.ai.robot.service;

/**
 * 记忆服务接口
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 记忆按会话（chat_uuid）隔离：结构化记忆的落库与清理都以单个会话为边界；
 * GLOBAL 层为系统预置的领域设定，不由对话写入。
 **/
public interface MemoryExtractionService {

    /**
     * 解析主对话模型附带的结构化记忆 JSON 并落库到当前会话
     *
     * @param chatUuid   会话 UUID（记忆归属边界）
     * @param userId     会话所属用户（作归属记录，登录体系就绪前为调试用户）
     * @param memoryJson 记忆 JSON（可能被标记包裹，也可直接传入）
     */
    void storeFromJson(String chatUuid, String userId, String memoryJson);

    /**
     * 删除某会话的一条私有固定槽记忆（用于"忘掉 XXX"等纠正请求）
     *
     * @param chatUuid 会话 UUID
     * @param slotType 槽类型
     * @param content  要忘掉的槽值内容
     */
    void deleteChatMemory(String chatUuid, String slotType, String content);

    /**
     * 清空某会话的所有私有固定槽记忆
     *
     * @param chatUuid 会话 UUID
     */
    void clearChatMemories(String chatUuid);
}
