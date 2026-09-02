package com.hhuly.ai.robot.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 固定槽记忆 DO 实体类
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 对应 t_user_memory 表；跨会话长期稳定的用户级记忆（偏好/语言/规则/禁忌）
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user_memory")
public class UserMemoryDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属范围：USER 本会话私有 / GLOBAL 系统预置领域设定 */
    private String scope;

    /** 归属用户（仅作归属记录：USER 记录为会话所属用户，GLOBAL 为空） */
    private String userId;

    /** 所属会话（scope = USER 时必填；GLOBAL 预置记录为空） */
    private String chatUuid;

    /** 槽类型：PREFERENCE / LANGUAGE / RULE / FORBIDDEN */
    private String slotType;

    /** 槽值 */
    private String content;

    /** 是否启用：软删除标记，覆盖/废弃时置 FALSE */
    private Boolean isActive;

    /** 溯源：哪场对话沉淀出这条 */
    private String sourceChatUuid;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
