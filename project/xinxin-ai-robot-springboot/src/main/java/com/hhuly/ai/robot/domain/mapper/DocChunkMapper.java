package com.hhuly.ai.robot.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hhuly.ai.robot.domain.dos.DocChunkDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档块文本 Mapper
 *
 * @author: li
 * @date: 2026/9/3
 **/
public interface DocChunkMapper extends BaseMapper<DocChunkDO> {

    /**
     * 关键词(topK)检索：jieba 已分词的 content_tokens 走 tsvector + ts_rank_cd。
     * 数据隔离：仅系统内置(owner=0) 或当前用户(owner=userId)。
     *
     * @param tsQuery "词1 & 词2" 形式的 tsquery（由 ChineseTokenizer 生成）
     * @param userId  当前登录用户；null 时仅内置文档
     * @param limit   返回条数
     */
    @Select("""
            SELECT id, doc_id, content, content_tokens, owner, md_storage_id, file_name
            FROM t_doc_chunk
            WHERE content_tsv @@ to_tsquery('simple', #{tsQuery})
              AND (owner = 0 OR owner = #{userId})
            ORDER BY ts_rank_cd(content_tsv, to_tsquery('simple', #{tsQuery})) DESC
            LIMIT #{limit}
            """)
    List<DocChunkDO> searchTopK(@Param("tsQuery") String tsQuery,
                                @Param("userId") Long userId,
                                @Param("limit") int limit);
}
