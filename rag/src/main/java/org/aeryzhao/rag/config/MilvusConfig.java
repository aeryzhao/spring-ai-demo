package org.aeryzhao.rag.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class MilvusConfig {

    @Value("${milvus.host}")
    private String host;

    @Value("${milvus.port}")
    private Integer port;

    @Value("${milvus.database}")
    private String database;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        log.info("Initializing Milvus client connection - Host: {}, Port: {}, Database: {}", host, port, database);
        
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withDatabaseName(database)
                .withConnectTimeout(10, TimeUnit.SECONDS)
                .withKeepAliveTime(60, TimeUnit.SECONDS)
                .withKeepAliveTimeout(20, TimeUnit.SECONDS)
                .build();

        MilvusServiceClient client = new MilvusServiceClient(connectParam);
        log.info("Milvus client initialized successfully");
        
        return client;
    }
}
