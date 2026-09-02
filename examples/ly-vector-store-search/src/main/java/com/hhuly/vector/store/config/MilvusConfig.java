package com.hhuly.vector.store.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.database.request.CreateDatabaseReq;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 连接配置
 */
@Configuration
public class MilvusConfig {

    @Bean(destroyMethod = "close")
    public MilvusClientV2 milvusClientV2(
            @Value("${milvus.uri}") String uri,
            @Value("${milvus.token:}") String token,
            @Value("${milvus.db-name:default}") String dbName,
            @Value("${milvus.connect-timeout-ms:10000}") long connectTimeoutMs) {

        // 先连接默认库，避免目标数据库不存在时初始化失败
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(uri)
                .token(token)
                .connectTimeoutMs(connectTimeoutMs)
                .build();
        MilvusClientV2 client = new MilvusClientV2(connectConfig);

        // 目标数据库不存在则自动创建，保证开箱即用
        if (!client.listDatabases().getDatabaseNames().contains(dbName)) {
            client.createDatabase(CreateDatabaseReq.builder().databaseName(dbName).build());
        }
        try {
            client.useDatabase(dbName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("切换 Milvus 数据库失败: " + dbName, e);
        }
        return client;
    }
}
