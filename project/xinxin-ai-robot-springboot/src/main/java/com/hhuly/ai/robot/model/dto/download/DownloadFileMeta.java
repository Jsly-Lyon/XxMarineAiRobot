package com.hhuly.ai.robot.model.dto.download;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 待下载文件元信息
 *
 * @author: li
 * @date: 2026/9/4
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadFileMeta {

    /** 展示用文件名 */
    private String fileName;
    /** 服务端文件绝对路径 */
    private String filePath;
}
