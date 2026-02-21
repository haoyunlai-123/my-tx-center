package com.my.tx.common;

import jakarta.servlet.http.HttpServletRequest;

public class TxHeaders {
    public static String key(HttpServletRequest req) {
        String txId = req.getHeader("X-TX-ID");
        String step = req.getHeader("X-STEP-INDEX");
        String phase = req.getHeader("X-PHASE");
        return (txId == null ? "" : txId) + "|" + (step == null ? "" : step) + "|" + (phase == null ? "" : phase);
    }
}