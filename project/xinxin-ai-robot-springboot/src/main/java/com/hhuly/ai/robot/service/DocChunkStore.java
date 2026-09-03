package com.hhuly.ai.robot.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hhuly.ai.robot.domain.dos.DocChunkDO;
import com.hhuly.ai.robot.domain.mapper.DocChunkMapper;
import com.hhuly.ai.robot.utils.ChineseTokenizer;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档块文本同步（关键词 BM25 路语料）
 *
 * @author: li
 * @date: 2026/9/3
 * @description: 与 Milvus 写入联动：将 Document 块同步到 t_doc_chunk（jieba 分词后），
 * 以 doc_id(内容哈希) 幂等 upsert；删除文件时按 md_storage_id 清理。
 **/
@Service
public class DocChunkStore {

    @Resource
    private DocChunkMapper docChunkMapper;

    /**
     * 同步一批块（幂等：按 doc_id upsert）
     *
     * @param docs       稳定 id 的 Document 块
     * @param owner      归属用户（系统内置传 0）
     * @param mdStorageId 上传文件表主键（内置文档传 null）
     * @param fileName   文件名
     */
    public void syncChunks(List<Document> docs, Long owner, Long mdStorageId, String fileName) {
        if (docs == null || docs.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Document doc : docs) {
            String docId = doc.getId();
            String content = doc.getText();
            if (!StringUtils.hasText(docId) || !StringUtils.hasText(content)) {
                continue;
            }
            String tokens = ChineseTokenizer.toSpaceText(content);

            DocChunkDO exist = docChunkMapper.selectOne(Wrappers.<DocChunkDO>lambdaQuery()
                    .eq(DocChunkDO::getDocId, docId));
            if (exist != null) {
                exist.setContent(content);
                exist.setContentTokens(tokens);
                exist.setOwner(owner != null ? owner : 0L);
                exist.setMdStorageId(mdStorageId);
                exist.setFileName(fileName);
                docChunkMapper.updateById(exist);
            } else {
                docChunkMapper.insert(DocChunkDO.builder()
                        .docId(docId)
                        .content(content)
                        .contentTokens(tokens)
                        .owner(owner != null ? owner : 0L)
                        .mdStorageId(mdStorageId)
                        .fileName(fileName)
                        .createTime(now)
                        .build());
            }
        }
    }

    /**
     * 删除某个上传文件对应的所有块（删除文件时联动）
     */
    public void deleteByMdStorageId(Long mdStorageId) {
        docChunkMapper.delete(Wrappers.<DocChunkDO>lambdaQuery()
                .eq(DocChunkDO::getMdStorageId, mdStorageId));
    }
}
