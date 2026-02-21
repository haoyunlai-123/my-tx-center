package com.my.tx.common.enums;

public enum TxStatus {
    NEW,
    RUNNING,
    SUCCEEDED,

    COMPENSATING,
    COMPENSATED,

    FAILED
}