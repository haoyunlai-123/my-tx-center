package com.my.tx.common;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DbIdempotencyService {

    private final JdbcTemplate jdbcTemplate;

    public DbIdempotencyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @return true=首次执行；false=重复请求（应直接返回成功）
     */
    public boolean firstTime(String idemKey) {
        try {
            jdbcTemplate.update("INSERT INTO tx_idempotency(idem_key) VALUES (?)", idemKey);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}