package com.my.tx.store.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 全局事务表
 */
@Data
public class TxGlobalDO {
    private Long id;
    private String txId;
    private String bizType;
    private String bizKey;
    private String status;
    private Integer currentStep;
    private String errorCode;
    private String errorMsg;

    private LocalDateTime nextRunAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long version;
}