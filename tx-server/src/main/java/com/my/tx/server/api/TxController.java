package com.my.tx.server.api;

import com.my.tx.server.api.dto.StartTxReq;
import com.my.tx.server.api.dto.StartTxResp;
import com.my.tx.server.service.TxStartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tx")
public class TxController {

    private final TxStartService txStartService;

    public TxController(TxStartService txStartService) {
        this.txStartService = txStartService;
    }

    @PostMapping("/start")
    public StartTxResp start(@RequestBody StartTxReq req) {
        String txId = txStartService.start(req);
        return new StartTxResp(txId);
    }
}