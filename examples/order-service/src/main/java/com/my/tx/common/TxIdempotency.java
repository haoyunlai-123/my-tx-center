package com.my.tx.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的事务幂等工具类，基于内存实现（重启会重置，但足够简历版）
 */
public class TxIdempotency {
    private static final long TTL_MS = 10 * 60 * 1000L; // 10分钟
    private static final Map<String, Long> seen = new ConcurrentHashMap<>();

    /** true 表示首次执行；false 表示重复请求，应直接返回成功（幂等） */
    public static boolean firstTime(String key) {
        long now = System.currentTimeMillis();
        cleanup(now);
        return seen.putIfAbsent(key, now) == null;
    }

    private static void cleanup(long now) {
        // 简易清理：每次调用顺便扫一遍（MVP 足够）
        for (Map.Entry<String, Long> e : seen.entrySet()) {
            if (now - e.getValue() > TTL_MS) {
                seen.remove(e.getKey());
            }
        }
    }
}