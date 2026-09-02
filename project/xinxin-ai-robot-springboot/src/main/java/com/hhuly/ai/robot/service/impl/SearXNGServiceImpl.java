package com.hhuly.ai.robot.service.impl;

import com.hhuly.ai.robot.model.dto.search.SearchResultDTO;
import com.hhuly.ai.robot.service.SearXNGService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class SearXNGServiceImpl implements SearXNGService {

    @Resource
    private OkHttpClient okHttpClient;
    @Resource
    private JsonMapper jsonMapper;
    @Value("${searxng.url}")
    private String searXNGUrl;
    @Value("${searxng.count}")
    private int searXNGCount;

    @Override
    public List<SearchResultDTO> search(String query) {
        // 构建 SearXNG API 请求 URL
        HttpUrl httpUrl = HttpUrl.parse(searXNGUrl).newBuilder()
                .addQueryParameter("q", query) // 设置搜索关键词
                .addQueryParameter("format", "json") // 指定返回 JSON 格式
                .addQueryParameter("engines", "yacy,abcnyheter,boardreader,wikimini,naver,vuhuv,crowdview,encyclosearch,resulthunter,yandex,fynd,ayo,reloado,searchmysite,bpb,quark,baidu,mwmbl,searchch,360search,bing") // 指定聚合的目标搜索引擎（配置本地网络能够访问的通的搜索引擎）
                .build();

        // 创建 HTTP GET 请求
        Request request = new Request.Builder()
                .url(httpUrl)
                .get()
                .build();

        // 发送 HTTP 请求
        try (Response response = okHttpClient.newCall(request).execute()) {
            // 判断请求是否成功
            if (response.isSuccessful()) {
                // 拿到返回结果
                String result = response.body().string();
                log.info("## SearXNG 搜索结果: {}", result);

                // 解析 JSON 响应
                JsonNode root = jsonMapper.readTree(result);
                JsonNode results = root.get("results"); // 获取结果数组节点

                // 定义 Record 类型：用于临时存储评分和节点引用
                record NodeWithUrlAndScore(double score, JsonNode node) {}

                // 处理搜索结果流：
                // 1. 提取评分
                // 2. 按评分降序排序
                // 3. 限制返回结果数量 (比如只提取评分最高的 50 条搜索结果)
                List<NodeWithUrlAndScore> nodesWithScore = StreamSupport.stream(results.spliterator(), false)
                        .map(node -> {
                            // 只提取分数，避免构建完整对象
                            double score = node.path("score").asDouble(0.0); // 提取评分
                            return new NodeWithUrlAndScore(score, node);
                        })
                        .sorted(Comparator.comparingDouble(NodeWithUrlAndScore::score).reversed()) // 按评分降序
                        .limit(searXNGCount) // 限制返回结果数量
                        .toList();

                // 转换为 SearchResult 对象集合
                return nodesWithScore.stream()
                        .map(n -> {
                            JsonNode node = n.node();
                            String originalUrl = node.path("url").asText(""); // 提取 URL
                            return SearchResultDTO.builder()
                                    .url(originalUrl)
                                    .score(n.score()) // 保留评分
                                    .build();
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("", e);
        }
        // 返回空集合
        return Collections.emptyList() ;
    }
}
