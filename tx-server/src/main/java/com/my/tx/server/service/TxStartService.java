package com.my.tx.server.service;

import com.my.tx.common.enums.StepStatus;
import com.my.tx.common.enums.TxStatus;
import com.my.tx.server.api.dto.StartTxReq;
import com.my.tx.store.mapper.TxGlobalMapper;
import com.my.tx.store.mapper.TxStepMapper;
import com.my.tx.store.model.TxGlobalDO;
import com.my.tx.store.model.TxStepDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TxStartService {

    private final TxGlobalMapper txGlobalMapper;
    private final TxStepMapper txStepMapper;

    public TxStartService(TxGlobalMapper txGlobalMapper, TxStepMapper txStepMapper) {
        this.txGlobalMapper = txGlobalMapper;
        this.txStepMapper = txStepMapper;
    }

    @Transactional
    public String start(StartTxReq req) {
        if (req == null || req.getSteps() == null || req.getSteps().isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
        String txId = "tx_" + UUID.randomUUID().toString().replace("-", "");

        TxGlobalDO g = new TxGlobalDO();
        g.setTxId(txId);
        g.setBizType(req.getBizType() == null ? "default" : req.getBizType());
        g.setBizKey(req.getBizKey() == null ? txId : req.getBizKey());
        g.setStatus(TxStatus.RUNNING.name());
        g.setCurrentStep(0);
        g.setNextRunAt(LocalDateTime.now());
        g.setVersion(0L);

        txGlobalMapper.insert(g);

        List<TxStepDO> steps = new ArrayList<>();
        int idx = 0;
        for (StartTxReq.Step s : req.getSteps()) {
            TxStepDO st = new TxStepDO();
            st.setTxId(txId);
            st.setStepIndex(idx++);
            st.setStepName(s.getStepName());

            st.setActionMethod(s.getActionMethod());
            st.setActionUrl(s.getActionUrl());
            st.setActionBody(s.getActionBody());

            st.setCompensateMethod(s.getCompensateMethod());
            st.setCompensateUrl(s.getCompensateUrl());
            st.setCompensateBody(s.getCompensateBody());

            st.setStatus(StepStatus.PENDING.name());
            st.setRetryCount(0);
            st.setRetryMax(s.getRetryMax());
            st.setTimeoutMs(s.getTimeoutMs());
            st.setLastError(null);

            steps.add(st);
        }
        txStepMapper.insertBatch(steps);

        return txId;
    }
}