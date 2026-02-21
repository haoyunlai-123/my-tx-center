package com.my.tx.server.api.dto;

import lombok.Data;

@Data
public class StartTxResp {
    private String txId;

    public StartTxResp() {}
    public StartTxResp(String txId) { this.txId = txId; }

}