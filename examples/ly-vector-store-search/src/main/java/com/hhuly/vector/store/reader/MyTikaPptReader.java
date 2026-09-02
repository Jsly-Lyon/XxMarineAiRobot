package com.hhuly.vector.store.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyTikaPptReader {

    @Value("classpath:/document/XX牌云感变频空调说明书.pptx")
    private Resource resource;

    public List<Document> loadPpt() {
        // 新建 TikaDocumentReader 阅读器
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
        // 读取并转换为 Document 文档集合
        List<Document> documents = tikaDocumentReader.get();

        // 文档分块
        // 使用自定义设置
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(1000) // 每个分块的目标 token 数
                .withMinChunkSizeChars(400) // 标点符号截断阶段的字符下限
                .withMinChunkLengthToEmbed(10) // 允许入库（embedding）的最小字符长度, 小于此长度的块会被直接丢弃，避免文本太短污染
                .withMaxNumChunks(5000) // 单个文档最多生成的块数, 超出部分被截断，防止超长文档导致块爆炸
                .withKeepSeparator(true) // 是否在分块边界保留分隔符（标点符号）
                .build();

        return splitter.apply(documents);
    }

}

