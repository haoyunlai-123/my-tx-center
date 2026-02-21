package com.my.tx.store.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事务步骤日志
 */
@Data
public class TxLogDO {
    private Long id;
    private String txId;
    private Integer stepIndex;
    private String phase;     // ACTION / COMPENSATE
    private Boolean success;
    private Integer costMs;
    private String message;
    private LocalDateTime createdAt;
}