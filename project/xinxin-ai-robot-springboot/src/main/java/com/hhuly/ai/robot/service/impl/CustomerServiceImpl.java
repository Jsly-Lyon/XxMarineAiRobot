package com.hhuly.ai.robot.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Maps;
import com.hhuly.ai.robot.domain.dos.AiCustomerServiceMdStorageDO;
import com.hhuly.ai.robot.domain.mapper.AiCustomerServiceMdStorageMapper;
import com.hhuly.ai.robot.enums.AiCustomerServiceMdStatusEnum;
import com.hhuly.ai.robot.enums.ResponseCodeEnum;
import com.hhuly.ai.robot.event.AiCustomerServiceMdUploadedEvent;
import com.hhuly.ai.robot.exception.BizException;
import com.hhuly.ai.robot.model.vo.customerService.DeleteMarkdownFileReqVO;
import com.hhuly.ai.robot.model.vo.customerService.FindMarkdownFilePageListReqVO;
import com.hhuly.ai.robot.model.vo.customerService.FindMarkdownFilePageListRspVO;
import com.hhuly.ai.robot.model.vo.customerService.UpdateMarkdownFileReqVO;
import com.hhuly.ai.robot.service.CustomerService;
import com.hhuly.ai.robot.utils.PageResponse;
import com.hhuly.ai.robot.utils.Response;
import com.hhuly.ai.robot.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    @Value("${customer-service.md-storage-path}")
    private String mdStoragePath;

    @Resource
    private AiCustomerServiceMdStorageMapper aiCustomerServiceMdStorageMapper;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private com.hhuly.ai.robot.service.DocChunkStore docChunkStore;

    @Override
    public Response<Long> uploadMarkdownFile(MultipartFile file) {
        // 当前登录用户（文件归属）
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResponseCodeEnum.NOT_LOGIN);
        }

        // 校验文件不能为空
        if (file == null || file.isEmpty()) {
            throw new BizException(ResponseCodeEnum.UPLOAD_FILE_CANT_EMPTY);
        }

        // 获取原始文件名（去除空格）
        String originalFilename = StringUtils.trimToEmpty(file.getOriginalFilename());

        // 验证文件类型，仅支持 Markdown
        if (StringUtils.isBlank(originalFilename) || !isMarkdownFile(originalFilename)) {
            throw new BizException(ResponseCodeEnum.ONLY_SUPPORT_MARKDOWN);
        }

        try {
            // 重新生成文件名（防止文件名冲突导致覆盖）
            String newFilename = UUID.randomUUID().toString() + "-" + originalFilename;

            // 构建存储路径
            Path storageDirectory = Paths.get(mdStoragePath);
            Path targetPath = storageDirectory.resolve(newFilename);

            // 确保目录存在并保存文件
            Files.createDirectories(storageDirectory);
            file.transferTo(targetPath.toFile());

            // 存储入库（归属当前用户，status = PENDING 待向量化）
            LocalDateTime now = LocalDateTime.now();
            AiCustomerServiceMdStorageDO storage = AiCustomerServiceMdStorageDO.builder()
                    .originalFileName(originalFilename)
                    .newFileName(newFilename)
                    .filePath(targetPath.toString())
                    .fileSize(file.getSize())
                    .userId(userId)
                    .status(AiCustomerServiceMdStatusEnum.PENDING.getCode())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            aiCustomerServiceMdStorageMapper.insert(storage);
            Long id = storage.getId();

            // 发布上传事件：携带 fileId 与元数据，由监听器异步向量化
            Map<String, Object> metadatas = Maps.newHashMap();
            metadatas.put("mdStorageId", id);           // 关联文件存储表主键 ID
            metadatas.put("originalFileName", originalFilename); // 文件原始名称

            eventPublisher.publishEvent(AiCustomerServiceMdUploadedEvent.builder()
                    .fileId(id)
                    .filePath(targetPath.toString())
                    .metadatas(metadatas)
                    .build());

            log.info("## Markdown 问答文件存储成功, 文件名: {} -> 路径: {}", originalFilename, targetPath);
            return Response.success(id);
        } catch (IOException e) {
            log.error("## Markdown 问答文件上传失败：{}", originalFilename, e);
            throw new BizException(ResponseCodeEnum.UPLOAD_FILE_FAILED);
        }
    }

    /**
     * 通用多格式文档上传（md/txt/doc(x)/ppt(x)/pdf/html）
     */
    @Override
    public Response<Long> uploadDocument(MultipartFile file) {
        // 当前登录用户（文件归属）
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResponseCodeEnum.NOT_LOGIN);
        }

        // 校验文件不能为空
        if (file == null || file.isEmpty()) {
            throw new BizException(ResponseCodeEnum.UPLOAD_FILE_CANT_EMPTY);
        }

        // 获取原始文件名（去除空格）
        String originalFilename = StringUtils.trimToEmpty(file.getOriginalFilename());

        // 校验文件类型（白名单：Markdown / TXT / Word / PPT / PDF / HTML）
        if (StringUtils.isBlank(originalFilename) || !isSupportedDocument(originalFilename)) {
            throw new BizException(ResponseCodeEnum.DOC_TYPE_NOT_SUPPORT);
        }

        try {
            // 重新生成文件名（防止文件名冲突导致覆盖）
            String newFilename = UUID.randomUUID().toString() + "-" + originalFilename;

            // 构建存储路径（沿用 md 存储目录）
            Path storageDirectory = Paths.get(mdStoragePath);
            Path targetPath = storageDirectory.resolve(newFilename);

            // 确保目录存在并保存文件
            Files.createDirectories(storageDirectory);
            file.transferTo(targetPath.toFile());

            // 存储入库（归属当前用户，status = PENDING 待向量化）
            LocalDateTime now = LocalDateTime.now();
            AiCustomerServiceMdStorageDO storage = AiCustomerServiceMdStorageDO.builder()
                    .originalFileName(originalFilename)
                    .newFileName(newFilename)
                    .filePath(targetPath.toString())
                    .fileSize(file.getSize())
                    .userId(userId)
                    .status(AiCustomerServiceMdStatusEnum.PENDING.getCode())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            aiCustomerServiceMdStorageMapper.insert(storage);
            Long id = storage.getId();

            // 发布上传事件：由监听器按扩展名选择解析器异步向量化
            Map<String, Object> metadatas = Maps.newHashMap();
            metadatas.put("mdStorageId", id);
            metadatas.put("originalFileName", originalFilename);

            eventPublisher.publishEvent(AiCustomerServiceMdUploadedEvent.builder()
                    .fileId(id)
                    .filePath(targetPath.toString())
                    .metadatas(metadatas)
                    .build());

            log.info("## 文档上传成功, 文件名: {} -> 路径: {}", originalFilename, targetPath);
            return Response.success(id);
        } catch (IOException e) {
            log.error("## 文档上传失败：{}", originalFilename, e);
            throw new BizException(ResponseCodeEnum.UPLOAD_FILE_FAILED);
        }
    }

    /**
     * 是否支持的多格式文档
     */
    private boolean isSupportedDocument(String filename) {
        String ext = FilenameUtils.getExtension(filename);
        if (ext == null || ext.isBlank()) {
            return false;
        }
        return switch (ext.toLowerCase(java.util.Locale.ROOT)) {
            case "md", "markdown", "txt", "text", "doc", "docx", "ppt", "pptx", "pdf", "html", "htm" -> true;
            default -> false;
        };
    }

    /**
     * 删除 Markdown 问答文件
     *
     * @param deleteMarkdownFileReqVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteMarkdownFile(DeleteMarkdownFileReqVO deleteMarkdownFileReqVO) {
        // 文件记录 ID
        Long id = deleteMarkdownFileReqVO.getId();

        // 查询该文件记录
        AiCustomerServiceMdStorageDO aiCustomerServiceMdStorageDO = aiCustomerServiceMdStorageMapper.selectById(id);

        // 若记录不存在
        if (Objects.isNull(aiCustomerServiceMdStorageDO)) {
            throw new BizException(ResponseCodeEnum.MARKDOWN_FILE_NOT_FOUND);
        }

        // 正在处理中的文件，无法删除
        AiCustomerServiceMdStatusEnum statusEnum = AiCustomerServiceMdStatusEnum.codeOf(aiCustomerServiceMdStorageDO.getStatus());
        if (Objects.equals(statusEnum, AiCustomerServiceMdStatusEnum.PENDING) // 待向量化
                || Objects.equals(statusEnum, AiCustomerServiceMdStatusEnum.VECTORIZING)) { // 向量化中...
            throw new BizException(ResponseCodeEnum.MARKDOWN_FILE_CANT_DELETE);
        }

        // 删除文件表记录
        aiCustomerServiceMdStorageMapper.deleteById(id);

        // 删除向量化数据
        vectorStore.delete(String.format("mdStorageId == %s", id));

        // 同步删除关键词语料块
        docChunkStore.deleteByMdStorageId(id);

        // 删除本地文件（hutool）
        String filePath = aiCustomerServiceMdStorageDO.getFilePath();
        try {
            FileUtil.del(filePath);
        } catch (Exception e) {
            log.error("## Markdown 问答文件删除失败：{}", filePath, e);
        }

        return Response.success();
    }


    /**
     * 归属校验：仅本人文件可见，否则视为不存在（不泄露他人文件信息）
     */
    private AiCustomerServiceMdStorageDO requireOwned(Long id) {
        AiCustomerServiceMdStorageDO storage = aiCustomerServiceMdStorageMapper.selectById(id);
        if (storage == null || !Objects.equals(storage.getUserId(), UserContext.getUserId())) {
            throw new BizException(ResponseCodeEnum.MD_FILE_NOT_EXISTED);
        }
        return storage;
    }

    /**
     * 分页查询 Markdown 问答文件
     *
     * @param findMarkdownFilePageListReqVO
     * @return
     */
    @Override
    public PageResponse<FindMarkdownFilePageListRspVO> findMarkdownFilePageList(FindMarkdownFilePageListReqVO findMarkdownFilePageListReqVO) {
        // 获取当前页、以及每页需要展示的数据数量
        Long current = findMarkdownFilePageListReqVO.getCurrent();
        Long size = findMarkdownFilePageListReqVO.getSize();

        // 执行分页查询
        Page<AiCustomerServiceMdStorageDO> mdStorageDOPage = aiCustomerServiceMdStorageMapper.selectPageList(current, size);

        List<AiCustomerServiceMdStorageDO> mdStorageDOS = mdStorageDOPage.getRecords();
        // DO 转 VO
        List<FindMarkdownFilePageListRspVO> vos = null;
        if (CollUtil.isNotEmpty(mdStorageDOS)) {
            vos = mdStorageDOS.stream()
                    .map(mdStorageDO -> FindMarkdownFilePageListRspVO.builder() // 构建返参 VO 实体类
                            .id(mdStorageDO.getId())
                            .originalFileName(mdStorageDO.getOriginalFileName())
                            .fileSize(DataSizeUtil.format(mdStorageDO.getFileSize())) // Hutool 工具库提供的字节转换
                            .status(mdStorageDO.getStatus())
                            .createTime(mdStorageDO.getCreateTime())
                            .updateTime(mdStorageDO.getUpdateTime())
                            .remark(mdStorageDO.getRemark())
                            .build())
                    .collect(Collectors.toList());
        }

        return PageResponse.success(mdStorageDOPage, vos);
    }

    /**
     * 修改  Markdown 问答文件信息
     *
     * @param updateMarkdownFileReqVO
     * @return
     */
    @Override
    public Response<?> updateMarkdownFile(UpdateMarkdownFileReqVO updateMarkdownFileReqVO) {
        // 文件 ID
        Long id = updateMarkdownFileReqVO.getId();
        // 备注
        String remark = updateMarkdownFileReqVO.getRemark();

        // 根据 ID 修改备注信息
        int count = aiCustomerServiceMdStorageMapper.updateById(AiCustomerServiceMdStorageDO.builder()
                .id(id)
                .remark(remark)
                .updateTime(LocalDateTime.now())
                .build());

        // 若影响的行数为 0， 说明该文件记录不存在
        if (count == 0 ) {
            throw new BizException(ResponseCodeEnum.MARKDOWN_FILE_NOT_FOUND);
        }

        return Response.success();
    }
    
    /**
     * 验证文件是否为 Markdown 格式
     */
    private boolean isMarkdownFile(String filename) {
        if (StringUtils.isBlank(filename)) {
            return false;
        }
        String extension = FilenameUtils.getExtension(filename);
        return StringUtils.equalsIgnoreCase(extension, "md");
    }
}
