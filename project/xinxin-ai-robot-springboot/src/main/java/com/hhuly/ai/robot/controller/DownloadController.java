package com.hhuly.ai.robot.controller;

import com.hhuly.ai.robot.model.dto.download.DownloadFileMeta;
import com.hhuly.ai.robot.model.vo.download.AiExportReqVO;
import com.hhuly.ai.robot.model.vo.download.AiExportRspVO;
import com.hhuly.ai.robot.model.vo.download.DownloadRecordRspVO;
import com.hhuly.ai.robot.service.DownloadService;
import com.hhuly.ai.robot.utils.PageResponse;
import com.hhuly.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 下载中心：上传文件 / AI 导出文件下载，并写入下载记录
 *
 * @author: li
 * @date: 2026/9/4
 **/
@RestController
@RequestMapping("/download")
@Slf4j
public class DownloadController {

    @Resource
    private DownloadService downloadService;

    /**
     * 下载上传的问答文件（下载即写入下载记录）
     */
    @GetMapping("/upload-file")
    public ResponseEntity<org.springframework.core.io.Resource> downloadUploadFile(@RequestParam Long fileId) {
        DownloadFileMeta meta = downloadService.downloadUploadFile(fileId);
        return streamFile(meta);
    }

    /**
     * 下载 AI 导出文件（按下载记录再次下载，仅本人）
     */
    @GetMapping("/record/{recordId}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadRecord(@PathVariable Long recordId) {
        DownloadFileMeta meta = downloadService.downloadByRecordId(recordId);
        return streamFile(meta);
    }

    /**
     * AI 回答导出为 Markdown（落盘 + 写入下载记录）
     */
    @PostMapping("/ai-export")
    public Response<AiExportRspVO> exportAiMarkdown(@RequestBody @Validated AiExportReqVO req) {
        return downloadService.exportAiMarkdown(req);
    }

    /**
     * 下载记录分页（仅本人）
     */
    @GetMapping("/list")
    public PageResponse<DownloadRecordRspVO> pageList(@RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        return downloadService.pageList(current, size);
    }

    private ResponseEntity<org.springframework.core.io.Resource> streamFile(DownloadFileMeta meta) {
        String encoded = URLEncoder.encode(meta.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        return ResponseEntity.ok()
                .headers(headers)
                .body(new FileSystemResource(meta.getFilePath()));
    }
}
