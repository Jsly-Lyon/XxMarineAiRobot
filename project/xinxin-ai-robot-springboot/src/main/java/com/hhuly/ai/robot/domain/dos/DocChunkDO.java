package com.hhuly.ai.robot.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档块文本 DO（关键词 BM25 路检索语料）
 *
 * @author: li
 * @date: 2026/9/3
 * @description: 与 Milvus 中的 Document 一一对应（doc_id = 内容哈希稳定 id）；
 * content_tokens 为 jieba 分词词串，配合 generated content_tsv 做 tsvector 关键词检索。
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_doc_chunk")
public class DocChunkDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 内容哈希稳定 ID（与 Milvus Document id 同源） */
    private String docId;

    /** 块文本 */
    private String content;

    /** jieba 分词后的空格词串 */
    private String contentTokens;

    /** 归属用户（0 = 系统内置） */
    private Long owner;

    /** 上传文件表主键（内置文档为空） */
    private Long mdStorageId;

    /** 文件名 */
    private String fileName;

    private LocalDateTime createTime;
}
