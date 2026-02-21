package com.my.tx.server.api;

import com.my.tx.common.enums.TxStatus;
import com.my.tx.server.api.dto.StartTxReq;
import com.my.tx.server.api.dto.StartTxResp;
import com.my.tx.server.api.dto.TxDetailResp;
import com.my.tx.server.service.TxQueryService;
import com.my.tx.server.service.TxStartService;
import com.my.tx.store.mapper.TxGlobalMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/tx")
public class TxController {

    private final TxStartService txStartService;
    private final TxQueryService txQueryService;
    private final TxGlobalMapper txGlobalMapper;

    public TxController(TxStartService txStartService,
                        TxQueryService txQueryService,
                        TxGlobalMapper txGlobalMapper) {
        this.txStartService = txStartService;
        this.txQueryService = txQueryService;
        this.txGlobalMapper = txGlobalMapper;
    }

    @PostMapping("/start")
    public StartTxResp start(@RequestBody StartTxReq req) {
        String txId = txStartService.start(req);
        return new StartTxResp(txId);
    }

    // 查询详情：/tx/{txId}?logLimit=50
    @GetMapping("/{txId}")
    public TxDetailResp detail(@PathVariable String txId,
                               @RequestParam(value = "logLimit", defaultValue = "50") int logLimit) {
        TxDetailResp resp = txQueryService.detail(txId, logLimit);
        if (resp == null) {
            throw new IllegalArgumentException("tx not found: " + txId);
        }
        return resp;
    }

    // 手动触发重试：把 next_run_at 置 now（worker 会马上推进）
    @PostMapping("/{txId}/retry")
    public String retry(@PathVariable String txId) {
        txGlobalMapper.updateNextRunAt(txId, LocalDateTime.now());
        return "OK";
    }

    // 手动进入补偿（遇到 RUNNING 卡住/想强制回滚时）
    @PostMapping("/{txId}/compensate")
    public String compensate(@PathVariable String txId) {
        txGlobalMapper.updateStatusAndError(txId, TxStatus.COMPENSATING.name(), "MANUAL", "manual compensate");
        txGlobalMapper.updateNextRunAt(txId, LocalDateTime.now());
        return "OK";
    }
}