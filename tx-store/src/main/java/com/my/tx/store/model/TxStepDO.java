package com.my.tx.store.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 事务步骤
 */
@Data
public class TxStepDO {
    private Long id;
    private String txId;
    private Integer stepIndex;
    private String stepName;

    private String actionMethod;
    private String actionUrl;
    private String actionBody;

    private String compensateMethod;
    private String compensateUrl;
    private String compensateBody;

    private String status;
    private Integer retryCount;
    private Integer retryMax;
    private Integer timeoutMs;

    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}