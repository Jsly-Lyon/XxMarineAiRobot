package com.hhuly.ai.robot.service;

import com.hhuly.ai.robot.model.dto.download.DownloadFileMeta;
import com.hhuly.ai.robot.model.vo.download.AiExportReqVO;
import com.hhuly.ai.robot.model.vo.download.AiExportRspVO;
import com.hhuly.ai.robot.model.vo.download.DownloadRecordRspVO;
import com.hhuly.ai.robot.utils.PageResponse;
import com.hhuly.ai.robot.utils.Response;

public interface DownloadService {

    /** 上传文件下载（校验本人并写入下载记录），返回可流式读取的文件信息 */
    DownloadFileMeta downloadUploadFile(Long fileId);

    /** 根据下载记录再次下载（仅本人），返回可流式读取的文件信息 */
    DownloadFileMeta downloadByRecordId(Long recordId);

    /** AI 回答导出 Markdown：落盘 + 写入下载记录 */
    Response<AiExportRspVO> exportAiMarkdown(AiExportReqVO req);

    /** 当前用户下载记录分页 */
    PageResponse<DownloadRecordRspVO> pageList(Long current, Long size);
}
