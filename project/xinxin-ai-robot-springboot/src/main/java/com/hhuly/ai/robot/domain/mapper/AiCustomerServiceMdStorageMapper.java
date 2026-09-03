package com.hhuly.ai.robot.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhuly.ai.robot.domain.dos.AiCustomerServiceMdStorageDO;

public interface AiCustomerServiceMdStorageMapper extends BaseMapper<AiCustomerServiceMdStorageDO> {

    /**
     * 分页查询
     * @param current
     * @param size
     * @return
             */
    default Page<AiCustomerServiceMdStorageDO> selectPageList(Long current, Long size) {
        // 分页对象(查询第几页、每页多少数据)
        Page<AiCustomerServiceMdStorageDO> page = new Page<>(current, size);

        // 构建查询条件
        LambdaQueryWrapper<AiCustomerServiceMdStorageDO> wrapper = Wrappers.<AiCustomerServiceMdStorageDO>lambdaQuery()
                .orderByDesc(AiCustomerServiceMdStorageDO::getCreateTime); // 按创建时间倒序

        return selectPage(page, wrapper);
    }
}
