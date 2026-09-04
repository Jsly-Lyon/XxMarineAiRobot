package com.hhuly.ai.robot.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhuly.ai.robot.domain.dos.AiCustomerServiceFileStorageDO;
import com.hhuly.ai.robot.domain.dos.FileChunkInfoDO;
import com.hhuly.ai.robot.domain.mapper.AiCustomerServiceFileStorageMapper;
import com.hhuly.ai.robot.domain.mapper.FileChunkInfoMapper;
import com.hhuly.ai.robot.enums.AiCustomerServiceFileStatusEnum;
import com.hhuly.ai.robot.enums.ResponseCodeEnum;
import com.hhuly.ai.robot.exception.BizException;
import com.hhuly.ai.robot.model.vo.customerService.CheckFileReqVO;
import com.hhuly.ai.robot.model.vo.customerService.CheckFileRspVO;
import com.hhuly.ai.robot.model.vo.customerService.DeleteMarkdownFileReqVO;
import com.hhuly.ai.robot.model.vo.customerService.FindMarkdownFilePageListReqVO;
import com.hhuly.ai.robot.model.vo.customerService.FindMarkdownFilePageListRspVO;
import com.hhuly.ai.robot.model.vo.customerService.MergeChunkReqVO;
import com.hhuly.ai.robot.model.vo.customerService.UpdateMarkdownFileReqVO;
import com.hhuly.ai.robot.model.vo.customerService.UploadChunkReqVO;
import com.hhuly.ai.robot.service.CustomerService;
import com.hhuly.ai.robot.service.DocChunkStore;
import com.hhuly.ai.robot.utils.PageResponse;
import com.hhuly.ai.robot.utils.Response;
import com.hhuly.ai.robot.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.hhuly.ai.robot.event.AiCustomerServiceMdUploadedEvent;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 智能客服文件服务实现
 *
 * <p>旧的单次整文件上传（uploadMarkdownFile / uploadDocument）已按分片上传方案移除，
 * 文件登记改为分片表 t_ai_customer_service_file_storage，上传/合并接口见后续小节。</p>
 *
 * @author: li
 * @date: 2026/9/3
 **/
@Service
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    @Value("${customer-service.file-storage-path}")
    private String fileStoragePath;

    @Value("${customer-service.chunk-path}")
    private String chunkPath;

    @Resource
    private AiCustomerServiceFileStorageMapper aiCustomerServiceFileStorageMapper;

    @Resource
    private FileChunkInfoMapper fileChunkInfoMapper;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private DocChunkStore docChunkStore;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 分片上传前：检查文件（秒传 / 断点续传）
     * 数据隔离：按「当前用户 + MD5」查询，他人同名文件不影响本人
     */
    @Override
    public Response<CheckFileRspVO> checkFile(CheckFileReqVO checkFileReqVO) {
        // 当前登录用户
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResponseCodeEnum.NOT_LOGIN);
        }
        String fileMd5 = checkFileReqVO.getFileMd5();

        // 查询当前用户下该 MD5 的文件记录
        AiCustomerServiceFileStorageDO fileStorageDO = aiCustomerServiceFileStorageMapper.selectByMd5(userId, fileMd5);

        // 记录不存在：需要上传
        if (Objects.isNull(fileStorageDO)) {
            return Response.success(CheckFileRspVO.builder()
                    .exists(false)
                    .needUpload(true)
                    .build());
        }

        // 记录存在：判断状态
        AiCustomerServiceFileStatusEnum statusEnum = AiCustomerServiceFileStatusEnum.codeOf(fileStorageDO.getStatus());

        // 已完成（非上传中）：支持秒传
        if (!Objects.equals(statusEnum, AiCustomerServiceFileStatusEnum.UPLOADING)) {
            return Response.success(CheckFileRspVO.builder()
                    .exists(true)
                    .needUpload(false)
                    .build());
        }

        // 上传中断未完成：返回已上传的分片序号，前端断点续传
        List<FileChunkInfoDO> chunks = fileChunkInfoMapper.selectChunkList(fileMd5);
        List<Integer> uploadedChunks = chunks.stream()
                .map(FileChunkInfoDO::getChunkNumber)
                .collect(Collectors.toList());

        return Response.success(CheckFileRspVO.builder()
                .exists(true)
                .needUpload(true)
                .uploadedChunks(uploadedChunks)
                .build());
    }

    /**
     * 文件分片上传（幂等：同一分片重复上传直接成功）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> uploadChunk(UploadChunkReqVO uploadChunkReqVO) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResponseCodeEnum.NOT_LOGIN);
        }

        String fileMd5 = uploadChunkReqVO.getFileMd5();
        Integer chunkNumber = uploadChunkReqVO.getChunkNumber();
        MultipartFile chunk = uploadChunkReqVO.getChunk();

        if (chunk == null || chunk.isEmpty()) {
            throw new BizException(ResponseCodeEnum.UPLOAD_FILE_CANT_EMPTY);
        }

        // 分片幂等：已上传过则直接返回
        Long count = fileChunkInfoMapper.selectCountByMd5AndChunkNum(fileMd5, chunkNumber);
        if (count != null && count > 0) {
            log.info("## 分片已存在: fileMd5={}, chunkNumber={}", fileMd5, chunkNumber);
            return Response.success();
        }

        // 分片临时目录：chunk-path/<fileMd5>
        String chunkDir = chunkPath + File.separator + fileMd5;
        File chunkDirFile = new File(chunkDir);
        try {
            FileUtils.forceMkdir(chunkDirFile);
        } catch (IOException e) {
            log.error("## 创建分片目录失败: {}", chunkDir, e);
            throw new RuntimeException(e);
        }

        // 保存分片文件
        String chunkFileName = chunkNumber + ".chunk";
        File chunkFile = new File(chunkDirFile, chunkFileName);
        try {
            chunk.transferTo(chunkFile);
        } catch (IOException e) {
            log.error("## 保存分片文件失败: {}", chunkFileName, e);
            throw new RuntimeException(e);
        }

        // 保存分片记录
        fileChunkInfoMapper.insert(FileChunkInfoDO.builder()
                .fileMd5(fileMd5)
                .chunkNumber(chunkNumber)
                .chunkPath(chunkFile.getAbsolutePath()) // 分片文件存储路径
                .chunkSize(chunk.getSize())
                .build());

        // 文件元记录登记：不存在则新建（首个分片，uploadedChunks=1），否则已上传分片数 +1
        AiCustomerServiceFileStorageDO fileStorageDO = aiCustomerServiceFileStorageMapper.selectByMd5(userId, fileMd5);
        if (fileStorageDO == null) {
            aiCustomerServiceFileStorageMapper.insert(AiCustomerServiceFileStorageDO.builder()
                    .fileMd5(fileMd5)
                    .fileName(uploadChunkReqVO.getFileName() != null ? uploadChunkReqVO.getFileName() : "")
                    .fileSize(uploadChunkReqVO.getFileSize() != null ? uploadChunkReqVO.getFileSize() : 0L)
                    .totalChunks(uploadChunkReqVO.getTotalChunks() != null ? uploadChunkReqVO.getTotalChunks() : 0)
                    .uploadedChunks(1) // 首个分片
                    .status(AiCustomerServiceFileStatusEnum.UPLOADING.getCode()) // 上传中
                    .filePath("") // 合并完成后回写
                    .userId(userId)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build());
        } else {
            aiCustomerServiceFileStorageMapper.incrementUploadedChunks(fileStorageDO.getId());
        }

        log.info("## 分片上传成功: fileMd5={}, chunkNumber={}", fileMd5, chunkNumber);
        return Response.success();
    }

    /**
     * 文件分片合并：校验分片完整后按序合并，更新文件信息并清理分片，最后发布事件触发异步向量化
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> mergeChunk(MergeChunkReqVO mergeChunkReqVO) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResponseCodeEnum.NOT_LOGIN);
        }

        String fileMd5 = mergeChunkReqVO.getFileMd5();

        // 校验文件元记录（本人）
        AiCustomerServiceFileStorageDO fileStorageDO = aiCustomerServiceFileStorageMapper.selectByMd5(userId, fileMd5);
        if (fileStorageDO == null) {
            throw new BizException(ResponseCodeEnum.MERGE_CHUNK_NOT_FOUND);
        }

        // 查询所有已上传分片（按序号升序）
        List<FileChunkInfoDO> chunks = fileChunkInfoMapper.selectChunkList(fileMd5);
        if (chunks.size() != fileStorageDO.getTotalChunks()) {
            throw new BizException(ResponseCodeEnum.CHUNK_NUM_NOT_COMPLETE);
        }

        // 合并目录
        File uploadDir = new File(fileStoragePath);
        try {
            FileUtils.forceMkdir(uploadDir);
        } catch (IOException e) {
            log.error("## 创建文件合并目录失败: {}", fileStoragePath, e);
            throw new RuntimeException(e);
        }

        // 合并后的最终文件
        String finalFileName = System.currentTimeMillis() + "_" + fileStorageDO.getFileName();
        File finalFile = new File(uploadDir, finalFileName);

        // 按分片序号顺序写入（8kb 缓冲，避免一次性加载所有分片到内存）
        try (FileOutputStream fos = new FileOutputStream(finalFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            for (FileChunkInfoDO chunkInfo : chunks) {
                File chunkFile = new File(chunkInfo.getChunkPath());
                try (FileInputStream fis = new FileInputStream(chunkFile);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                }
            }
        } catch (Exception e) {
            log.error("## 合并文件失败: ", e);
            throw new RuntimeException(e);
        }

        // 更新文件信息：合并完成 -> 待向量化
        aiCustomerServiceFileStorageMapper.updateById(AiCustomerServiceFileStorageDO.builder()
                .id(fileStorageDO.getId())
                .status(AiCustomerServiceFileStatusEnum.PENDING.getCode())
                .filePath(finalFile.getAbsolutePath())
                .updateTime(LocalDateTime.now())
                .build());

        // 清理分片临时文件目录与分片记录
        String chunkDir = chunkPath + File.separator + fileMd5;
        try {
            FileUtils.forceDelete(new File(chunkDir));
        } catch (IOException e) {
            log.error("## 删除分片文件失败: {}", chunkDir, e);
            throw new RuntimeException(e);
        }
        fileChunkInfoMapper.deleteByMd5(fileMd5);

        log.info("## 文件合并成功: fileMd5={}, filePath={}", fileMd5, finalFile.getAbsolutePath());

        // 合并完成后发布事件，触发异步向量化（监听器 AFTER_COMMIT 执行，确保数据已提交）
        Map<String, Object> metadatas = Maps.newHashMap();
        metadatas.put("mdStorageId", fileStorageDO.getId());
        metadatas.put("originalFileName", fileStorageDO.getFileName());
        eventPublisher.publishEvent(AiCustomerServiceMdUploadedEvent.builder()
                .fileId(fileStorageDO.getId())
                .filePath(finalFile.getAbsolutePath())
                .metadatas(metadatas)
                .build());

        return Response.success();
    }

    /**
     * 删除问答文件：本人记录删除 + 尽力清理向量/语料/本地文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteMarkdownFile(DeleteMarkdownFileReqVO deleteMarkdownFileReqVO) {
        // 归属校验：不存在或非本人一律视为不存在，避免越权
        Long id = deleteMarkdownFileReqVO.getId();
        AiCustomerServiceFileStorageDO storage = requireOwned(id);

        // 正在处理中的文件，无法删除（待向量化 / 向量化中）
        AiCustomerServiceFileStatusEnum statusEnum = AiCustomerServiceFileStatusEnum.codeOf(storage.getStatus());
        if (Objects.equals(statusEnum, AiCustomerServiceFileStatusEnum.PENDING)
                || Objects.equals(statusEnum, AiCustomerServiceFileStatusEnum.VECTORIZING)) {
            throw new BizException(ResponseCodeEnum.MARKDOWN_FILE_CANT_DELETE);
        }

        // 删除文件表记录
        aiCustomerServiceFileStorageMapper.deleteById(id);

        // 清理向量库（尽力而为：Milvus 未就绪时不阻断文件删除，仅告警）
        try {
            vectorStore.delete(String.format("mdStorageId == %s", id));
        } catch (Exception e) {
            log.warn("## 清理向量失败（文件已删除，后续可修复向量库后清理）: id={}", id, e);
        }

        // 同步删除关键词语料块（同一数据库事务，失败则整体回滚）
        docChunkStore.deleteByMdStorageId(id);

        // 清理分片临时文件目录与分片记录（上传中断/未合并时也会残留）
        String fileMd5 = storage.getFileMd5();
        if (fileMd5 != null) {
            FileUtil.del(chunkPath + File.separator + fileMd5); // hutool：目录不存在时静默忽略
            fileChunkInfoMapper.deleteByMd5(fileMd5);
        }

        // 删除本地文件（hutool，失败仅记录日志不中断）
        String filePath = storage.getFilePath();
        try {
            FileUtil.del(filePath);
        } catch (Exception e) {
            log.error("## 问答文件本地删除失败：{}", filePath, e);
        }

        return Response.success();
    }

    /**
     * 归属校验：仅本人文件可见，否则视为不存在（不泄露他人文件信息）
     */
    private AiCustomerServiceFileStorageDO requireOwned(Long id) {
        AiCustomerServiceFileStorageDO storage = aiCustomerServiceFileStorageMapper.selectById(id);
        if (storage == null || !Objects.equals(storage.getUserId(), UserContext.getUserId())) {
            throw new BizException(ResponseCodeEnum.MD_FILE_NOT_EXISTED);
        }
        return storage;
    }

    /**
     * 分页查询问答文件（仅本人）
     */
    @Override
    public PageResponse<FindMarkdownFilePageListRspVO> findMarkdownFilePageList(FindMarkdownFilePageListReqVO findMarkdownFilePageListReqVO) {
        // 获取当前页、以及每页需要展示的数据数量
        Long current = findMarkdownFilePageListReqVO.getCurrent();
        Long size = findMarkdownFilePageListReqVO.getSize();

        // 条件查询：文件名模糊 + 创建时间段
        String fileName = findMarkdownFilePageListReqVO.getFileName();
        java.time.LocalDate startDate = findMarkdownFilePageListReqVO.getStartDate();
        java.time.LocalDate endDate = findMarkdownFilePageListReqVO.getEndDate();

        // 数据隔离：仅当前登录用户本人文件
        Long userId = UserContext.getUserId();

        // 执行分页查询
        Page<AiCustomerServiceFileStorageDO> storageDOPage =
                aiCustomerServiceFileStorageMapper.selectPageList(current, size, fileName, startDate, endDate, userId);

        List<AiCustomerServiceFileStorageDO> storageDOS = storageDOPage.getRecords();
        // DO 转 VO
        List<FindMarkdownFilePageListRspVO> vos = null;
        if (CollUtil.isNotEmpty(storageDOS)) {
            vos = storageDOS.stream()
                    .map(storageDO -> FindMarkdownFilePageListRspVO.builder() // 构建返参 VO 实体类
                            .id(storageDO.getId())
                            .fileName(storageDO.getFileName())
                            .fileSize(DataSizeUtil.format(storageDO.getFileSize())) // Hutool 工具库提供的字节转换
                            .status(storageDO.getStatus())
                            .createTime(storageDO.getCreateTime())
                            .updateTime(storageDO.getUpdateTime())
                            .remark(storageDO.getRemark())
                            .build())
                    .collect(Collectors.toList());
        }

        return PageResponse.success(storageDOPage, vos);
    }

    /**
     * 修改问答文件信息（仅备注，仅本人）
     */
    @Override
    public Response<?> updateMarkdownFile(UpdateMarkdownFileReqVO updateMarkdownFileReqVO) {
        // 归属校验
        Long id = updateMarkdownFileReqVO.getId();
        requireOwned(id);
        // 备注
        String remark = updateMarkdownFileReqVO.getRemark();

        // 根据 ID 修改备注信息
        int count = aiCustomerServiceFileStorageMapper.updateById(AiCustomerServiceFileStorageDO.builder()
                .id(id)
                .remark(remark)
                .updateTime(LocalDateTime.now())
                .build());

        // 若影响的行数为 0，说明该文件记录不存在
        if (count == 0) {
            throw new BizException(ResponseCodeEnum.MARKDOWN_FILE_NOT_FOUND);
        }

        return Response.success();
    }
}
