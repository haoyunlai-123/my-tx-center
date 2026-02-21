package com.my.tx.server.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        // 连接池：最大 50 个空闲连接，5 分钟 keep-alive
        ConnectionPool pool = new ConnectionPool(50, 5, TimeUnit.MINUTES);

        return new OkHttpClient.Builder()
                .connectionPool(pool)
                .retryOnConnectionFailure(false) // 重试由 SagaEngine 控制（更可控）
                .connectTimeout(Duration.ofMillis(800))
                .readTimeout(Duration.ofMillis(3000))
                .writeTimeout(Duration.ofMillis(3000))
                .callTimeout(Duration.ofMillis(3500))
                .build();
    }
}