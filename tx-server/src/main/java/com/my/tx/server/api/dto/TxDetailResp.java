package com.my.tx.server.api.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TxDetailResp {

    private Global global;
    private List<Step> steps;
    private List<Log> logs;

    public static class Global {
        public String txId;
        public String bizType;
        public String bizKey;
        public String status;
        public Integer currentStep;
        public String errorCode;
        public String errorMsg;
        public LocalDateTime nextRunAt;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
        public Long version;
    }

    public static class Step {
        public Integer stepIndex;
        public String stepName;

        public String status;
        public Integer retryCount;
        public Integer retryMax;
        public Integer timeoutMs;
        public String lastError;

        public String actionMethod;
        public String actionUrl;
        public String actionBody;

        public String compensateMethod;
        public String compensateUrl;
        public String compensateBody;
    }

    public static class Log {
        public Long id;
        public Integer stepIndex;
        public String phase;
        public Boolean success;
        public Integer costMs;
        public String message;
        public LocalDateTime createdAt;
    }

}