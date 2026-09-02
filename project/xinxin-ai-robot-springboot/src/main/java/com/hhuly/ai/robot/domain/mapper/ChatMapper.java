package com.hhuly.ai.robot.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhuly.ai.robot.domain.dos.ChatDO;

/**
 * @Author: li
 * @Date: 2026/8/31 11:32
 * @Version: v1.0.0
 * @Description: 对话 Mapper 接口
 **/
public interface ChatMapper extends BaseMapper<ChatDO> {

    /**
     * 分页查询（仅当前登录用户自己的会话）
     * @param current
     * @param size
     * @param userId 归属用户
     * @return
     */
    default Page<ChatDO> selectPageList(Long current, Long size, Long userId) {
        // 分页对象(查询第几页、每页多少数据)
        Page<ChatDO> page = new Page<>(current, size);

        // 构建查询条件
        LambdaQueryWrapper<ChatDO> wrapper = Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getUserId, userId) // 数据隔离：仅本人
                .orderByDesc(ChatDO::getUpdateTime); // 按更新时间倒序

        return selectPage(page, wrapper);
    }
}
