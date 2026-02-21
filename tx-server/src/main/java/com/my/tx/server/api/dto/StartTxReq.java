package com.my.tx.server.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class StartTxReq {
    private String bizType;
    private String bizKey;
    private List<Step> steps;

    @Data
    public static class Step {
        private String stepName;

        private String actionMethod = "POST";
        private String actionUrl;
        private String actionBody;

        private String compensateMethod = "POST";
        private String compensateUrl;
        private String compensateBody;

        private Integer retryMax = 3;
        private Integer timeoutMs = 3000;
    }

}