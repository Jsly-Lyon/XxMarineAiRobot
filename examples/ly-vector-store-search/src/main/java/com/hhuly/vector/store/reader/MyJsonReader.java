package com.hhuly.vector.store.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyJsonReader {

    @Value("classpath:/document/tv.json")
    private Resource resource;

    /**
     * 读取 Json 文件
     * @return
     */
    public List<Document> loadJson() {
        // 创建 JsonReader 阅读器实例，配置需要读取的字段
        JsonReader jsonReader = new JsonReader(resource, "description", "content", "title");
        // 执行读取操作，并转换为 Document 对象集合
        return jsonReader.get();
    }

}