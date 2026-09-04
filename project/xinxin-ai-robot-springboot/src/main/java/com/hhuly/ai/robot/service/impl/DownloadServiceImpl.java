package com.hhuly.ai.robot.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhuly.ai.robot.domain.dos.AiCustomerServiceFileStorageDO;
import com.hhuly.ai.robot.domain.dos.DownloadRecordDO;
import com.hhuly.ai.robot.domain.mapper.AiCustomerServiceFileStorageMapper;
import com.hhuly.ai.robot.domain.mapper.DownloadRecordMapper;
import com.hhuly.ai.robot.enums.ResponseCodeEnum;
import com.hhuly.ai.robot.exception.BizException;
import com.hhuly.ai.robot.model.dto.download.DownloadFileMeta;
import com.hhuly.ai.robot.model.vo.download.AiExportReqVO;
import com.hhuly.ai.robot.model.vo.download.AiExportRspVO;
import com.hhuly.ai.robot.model.vo.download.DownloadRecordRspVO;
import com.hhuly.ai.robot.service.DownloadService;
import com.hhuly.ai.robot.utils.PageResponse;
import com.hhuly.ai.robot.utils.Response;
import com.hhuly.ai.robot.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 下载中心：上传文件/AI 导出文件统一下载，并写入下载记录
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Slf4j
@Service
public class DownloadServiceImpl implements DownloadService {

    public static final String SOURCE_TYPE_UPLOAD = "UPLOAD";
    public static final String SOURCE_TYPE_AI_EXPORT = "AI_EXPORT";

    @Resource
    private AiCustomerServiceFileStorageMapper fileStorageMapper;
    @Resource
    private DownloadRecordMapper downloadRecordMapper;

    /** AI 导出文件存放目录 */
    @Value("${customer-service.export-path:}")
    private String exportPath;

    @Override
    public DownloadFileMeta downloadUploadFile(Long fileId) {
        Long userId = requireLogin();
        AiCustomerServiceFileStorageDO file = fileStorageMapper.selectById(fileId);
        if (file == null || !Objects.equals(file.getUserId(), userId)) {
            throw new BizException(ResponseCodeEnum.FILE_NOT_EXISTED);
        }
        if (!StringUtils.hasText(file.getFilePath())) {
            throw new BizException(ResponseCodeEnum.FILE_NOT_EXISTED);
        }

        // 记录一次下载
        downloadRecordMapper.insert(DownloadRecordDO.builder()
                .userId(userId)
                .sourceType(SOURCE_TYPE_UPLOAD)
                .fileName(file.getFileName())
                .filePath(file.getFilePath())
                .fileSize(file.getFileSize())
                .sourceId(file.getId())
                .createTime(LocalDateTime.now())
                .build());

        return DownloadFileMeta.builder().fileName(file.getFileName()).filePath(file.getFilePath()).build();
    }

    @Override
    public DownloadFileMeta downloadByRecordId(Long recordId) {
        Long userId = requireLogin();
        DownloadRecordDO record = downloadRecordMapper.selectById(recordId);
        if (record == null || !Objects.equals(record.getUserId(), userId)) {
            throw new BizException(ResponseCodeEnum.DOWNLOAD_RECORD_NOT_EXISTED);
        }
        if (!StringUtils.hasText(record.getFilePath())) {
            throw new BizException(ResponseCodeEnum.DOWNLOAD_RECORD_NOT_EXISTED);
        }
        return DownloadFileMeta.builder().fileName(record.getFileName()).filePath(record.getFilePath()).build();
    }

    @Override
    public Response<AiExportRspVO> exportAiMarkdown(AiExportReqVO req) {
        Long userId = requireLogin();
        try {
            String content = req.getContent() == null ? "" : req.getContent();
            String fileName = normalizeExportFileName(req.getFileName());
            Path dir = Paths.get(exportPath);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Files.write(target, content.getBytes(StandardCharsets.UTF_8));

            DownloadRecordDO record = DownloadRecordDO.builder()
                    .userId(userId)
                    .sourceType(SOURCE_TYPE_AI_EXPORT)
                    .fileName(fileName)
                    .filePath(target.toString())
                    .fileSize(Files.size(target))
                    .createTime(LocalDateTime.now())
                    .build();
            downloadRecordMapper.insert(record);

            return Response.success(AiExportRspVO.builder()
                    .recordId(record.getId())
                    .fileName(fileName)
                    .build());
        } catch (Exception ex) {
            log.error("## AI 导出 Markdown 失败", ex);
            throw new BizException(ResponseCodeEnum.UPLOAD_FILE_FAILED);
        }
    }

    @Override
    public PageResponse<DownloadRecordRspVO> pageList(Long current, Long size) {
        Long userId = requireLogin();
        Page<DownloadRecordDO> page = downloadRecordMapper.selectPageList(current, size, userId);
        List<DownloadRecordRspVO> vos = null;
        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            vos = page.getRecords().stream()
                    .map(r -> DownloadRecordRspVO.builder()
                            .id(r.getId())
                            .sourceType(r.getSourceType())
                            .fileName(r.getFileName())
                            .fileSize(r.getFileSize())
                            .createTime(r.getCreateTime())
                            .build())
                    .toList();
        }
        return PageResponse.success(page, vos);
    }

    private String normalizeExportFileName(String name) {
        String base = StringUtils.hasText(name) ? name.trim() : "";
        if (!StringUtils.hasText(base)) {
            base = "AI-回答";
        }
        if (!base.toLowerCase().endsWith(".md")) {
            base = base + ".md";
        }
        String safe = base.replaceAll("[\\\\/:*?\"<>|]", "-");
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int dot = safe.lastIndexOf('.');
        String stem = dot >= 0 ? safe.substring(0, dot) : safe;
        String ext = dot >= 0 ? safe.substring(dot) : ".md";
        return stem + "-" + ts + "-" + UUID.randomUUID().toString().substring(0, 6) + ext;
    }

    private Long requireLogin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResponseCodeEnum.NOT_LOGIN);
        }
        return userId;
    }
}
