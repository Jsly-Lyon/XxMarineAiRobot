package com.hhuly.ai.robot.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhuly.ai.robot.domain.dos.DownloadRecordDO;

public interface DownloadRecordMapper extends BaseMapper<DownloadRecordDO> {

    /** 分页查询某用户下载记录（按时间倒序） */
    default Page<DownloadRecordDO> selectPageList(Long current, Long size, Long userId) {
        Page<DownloadRecordDO> page = new Page<>(current, size);
        LambdaQueryWrapper<DownloadRecordDO> wrapper = Wrappers.<DownloadRecordDO>lambdaQuery()
                .eq(DownloadRecordDO::getUserId, userId)
                .orderByDesc(DownloadRecordDO::getCreateTime);
        return selectPage(page, wrapper);
    }
}
