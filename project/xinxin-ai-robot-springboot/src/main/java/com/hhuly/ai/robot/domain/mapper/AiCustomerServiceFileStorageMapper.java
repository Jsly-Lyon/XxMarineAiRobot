package com.hhuly.ai.robot.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhuly.ai.robot.domain.dos.AiCustomerServiceFileStorageDO;

import java.time.LocalDate;

/**
 * 客服问答文件存储 Mapper
 *
 * @author: li
 * @date: 2026/9/4
 **/
public interface AiCustomerServiceFileStorageMapper extends BaseMapper<AiCustomerServiceFileStorageDO> {

    /**
     * 根据文件 MD5 值 + 归属用户查询（隔离：仅本人）
     *
     * @param userId  归属用户 ID
     * @param fileMd5 文件 MD5
     * @return 文件记录（可能不存在）
     */
    default AiCustomerServiceFileStorageDO selectByMd5(Long userId, String fileMd5) {
        return selectOne(Wrappers.<AiCustomerServiceFileStorageDO>lambdaQuery()
                .eq(AiCustomerServiceFileStorageDO::getFileMd5, fileMd5)
                .eq(userId != null, AiCustomerServiceFileStorageDO::getUserId, userId));
    }

    /**
     * 分页查询（归属校验 + 文件名模糊 + 创建时间段）
     *
     * @param current   页码
     * @param size      每页条数
     * @param fileName  文件名（模糊，可空）
     * @param startDate 创建起始日（可空）
     * @param endDate   创建结束日（可空）
     * @param userId    归属用户 ID（数据隔离，仅本人）
     * @return 分页结果
     */
    default Page<AiCustomerServiceFileStorageDO> selectPageList(Long current, Long size,
                                                                String fileName, LocalDate startDate,
                                                                LocalDate endDate, Long userId) {
        Page<AiCustomerServiceFileStorageDO> page = new Page<>(current, size);

        // 注意：条件方法的 value 实参会先于 condition 求值，null 时需三元兜底，避免 NPE
        LambdaQueryWrapper<AiCustomerServiceFileStorageDO> wrapper = Wrappers.<AiCustomerServiceFileStorageDO>lambdaQuery()
                .eq(userId != null, AiCustomerServiceFileStorageDO::getUserId, userId) // 数据隔离：仅本人
                .like(fileName != null && !fileName.isBlank(), AiCustomerServiceFileStorageDO::getFileName, fileName)
                .ge(startDate != null, AiCustomerServiceFileStorageDO::getCreateTime, startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, AiCustomerServiceFileStorageDO::getCreateTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(AiCustomerServiceFileStorageDO::getCreateTime); // 按创建时间倒序

        return selectPage(page, wrapper);
    }

    /**
     * 已上传分片数 +1（分片上传成功后原子递增）
     *
     * @param id 文件记录主键
     * @return 影响行数
     */
    default int incrementUploadedChunks(Long id) {
        return update(Wrappers.<AiCustomerServiceFileStorageDO>lambdaUpdate()
                .eq(AiCustomerServiceFileStorageDO::getId, id)
                .setSql("uploaded_chunks = uploaded_chunks + 1"));
    }
}
