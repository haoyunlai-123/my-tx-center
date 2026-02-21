package com.my.tx.server.service;

import com.my.tx.server.api.dto.TxDetailResp;
import com.my.tx.store.mapper.TxGlobalMapper;
import com.my.tx.store.mapper.TxLogMapper;
import com.my.tx.store.mapper.TxStepMapper;
import com.my.tx.store.model.TxGlobalDO;
import com.my.tx.store.model.TxLogDO;
import com.my.tx.store.model.TxStepDO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TxQueryService {

    private final TxGlobalMapper txGlobalMapper;
    private final TxStepMapper txStepMapper;
    private final TxLogMapper txLogMapper;

    public TxQueryService(TxGlobalMapper txGlobalMapper, TxStepMapper txStepMapper, TxLogMapper txLogMapper) {
        this.txGlobalMapper = txGlobalMapper;
        this.txStepMapper = txStepMapper;
        this.txLogMapper = txLogMapper;
    }

    public TxDetailResp detail(String txId, int logLimit) {
        TxGlobalDO g = txGlobalMapper.selectByTxId(txId);
        if (g == null) return null;

        List<TxStepDO> steps = txStepMapper.selectByTxId(txId);
        List<TxLogDO> logs = txLogMapper.selectByTxId(txId, Math.max(1, logLimit));

        TxDetailResp resp = new TxDetailResp();

        TxDetailResp.Global gg = new TxDetailResp.Global();
        gg.txId = g.getTxId();
        gg.bizType = g.getBizType();
        gg.bizKey = g.getBizKey();
        gg.status = g.getStatus();
        gg.currentStep = g.getCurrentStep();
        gg.errorCode = g.getErrorCode();
        gg.errorMsg = g.getErrorMsg();
        gg.nextRunAt = g.getNextRunAt();
        gg.createdAt = g.getCreatedAt();
        gg.updatedAt = g.getUpdatedAt();
        gg.version = g.getVersion();
        resp.setGlobal(gg);

        List<TxDetailResp.Step> ss = new ArrayList<>();
        for (TxStepDO s : steps) {
            TxDetailResp.Step x = new TxDetailResp.Step();
            x.stepIndex = s.getStepIndex();
            x.stepName = s.getStepName();

            x.status = s.getStatus();
            x.retryCount = s.getRetryCount();
            x.retryMax = s.getRetryMax();
            x.timeoutMs = s.getTimeoutMs();
            x.lastError = s.getLastError();

            x.actionMethod = s.getActionMethod();
            x.actionUrl = s.getActionUrl();
            x.actionBody = s.getActionBody();

            x.compensateMethod = s.getCompensateMethod();
            x.compensateUrl = s.getCompensateUrl();
            x.compensateBody = s.getCompensateBody();

            ss.add(x);
        }
        resp.setSteps(ss);

        List<TxDetailResp.Log> ll = new ArrayList<>();
        for (TxLogDO l : logs) {
            TxDetailResp.Log y = new TxDetailResp.Log();
            y.id = l.getId();
            y.stepIndex = l.getStepIndex();
            y.phase = l.getPhase();
            y.success = l.getSuccess();
            y.costMs = l.getCostMs();
            y.message = l.getMessage();
            y.createdAt = l.getCreatedAt();
            ll.add(y);
        }
        resp.setLogs(ll);

        return resp;
    }
}