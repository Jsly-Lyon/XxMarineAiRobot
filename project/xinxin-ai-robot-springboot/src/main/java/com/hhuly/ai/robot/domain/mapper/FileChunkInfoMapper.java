package com.hhuly.ai.robot.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hhuly.ai.robot.domain.dos.FileChunkInfoDO;

import java.util.List;

/**
 * 分片信息 Mapper
 *
 * @author: li
 * @date: 2026/9/4
 **/
public interface FileChunkInfoMapper extends BaseMapper<FileChunkInfoDO> {

    /**
     * 根据文件 MD5 值查询所有已上传的分片（按分片序号升序）
     *
     * @param fileMd5 文件 MD5
     * @return 已上传分片列表
     */
    default List<FileChunkInfoDO> selectChunkList(String fileMd5) {
        return selectList(
                Wrappers.<FileChunkInfoDO>lambdaQuery()
                        .eq(FileChunkInfoDO::getFileMd5, fileMd5)
                        .orderByAsc(FileChunkInfoDO::getChunkNumber)
        );
    }

    /**
     * 查询指定分片是否已被上传
     *
     * @param fileMd5  文件 MD5
     * @param chunkNum 分片序号
     * @return 命中记录数（0=未上传）
     */
    default Long selectCountByMd5AndChunkNum(String fileMd5, Integer chunkNum) {
        return selectCount(
                Wrappers.<FileChunkInfoDO>lambdaQuery()
                        .eq(FileChunkInfoDO::getFileMd5, fileMd5)
                        .eq(FileChunkInfoDO::getChunkNumber, chunkNum)
        );
    }

    /**
     * 根据文件 MD5 删除分片记录（合并完成后清理）
     *
     * @param fileMd5 文件 MD5
     * @return 影响行数
     */
    default int deleteByMd5(String fileMd5) {
        return delete(Wrappers.<FileChunkInfoDO>lambdaQuery()
                .eq(FileChunkInfoDO::getFileMd5, fileMd5));
    }
}
