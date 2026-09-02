package com.hhuly.vector.store.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: li
 * @Date: 2026/7/18 11:12
 * @Version: v1.0.0
 * @Description: Txt 文件读取
 **/
@Component
public class MyTextReader {

    @Value("classpath:/document/manual.txt")
    private Resource resource;

    /**
     * 读取 Txt 文档
     * @return
     */
    public List<Document> loadText() {
        // 创建 TextReader 对象，用于读取指定资源 (resource) 的文本内容
        TextReader textReader = new TextReader(resource);
        // 添加自定义元数据，如文件名称
        textReader.getCustomMetadata()
                .put("filename", "manual.txt");
        // 读取并转换为 Document 文档集合
        return textReader.read();
    }

    /**
     * 读取 Txt 文档并分块拆分
     * @return
     */
    public List<Document> loadTextAndSplit() {
        // 创建 TextReader 对象，用于读取指定资源 (resource) 的文本内容
        TextReader textReader = new TextReader(resource);

        // 将资源内容解析为 Document 对象集合
        List<Document> documents = textReader.get();

        // 使用 TokenTextSplitter 对文档列表进行分块处理
        List<Document> splitDocuments = TokenTextSplitter.builder()
                .withChunkSize(50) // 每个分块的目标 token 数
                .withMinChunkSizeChars(20) // 标点符号截断阶段的字符下限
                .withMinChunkLengthToEmbed(10) // 允许入库（embedding）的最小字符长度, 小于此长度的块会被直接丢弃，避免文本太短污染
                .withMaxNumChunks(5000)  // 单个文档最多生成的块数, 超出部分被截断，防止超长文档导致块爆炸
                .withKeepSeparator(true) // 是否在分块边界保留分隔符（标点符号）
                .build()
                .apply(documents);

        // 返回拆分后的文档分块集合
        return splitDocuments;
    }

}
