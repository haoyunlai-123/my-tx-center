package com.my.tx.store.repository;

import com.my.tx.store.model.TxStepDO;

import java.util.List;

public interface TxStepRepository {

    int batchInsert(List<TxStepDO> rows);

    List<TxStepDO> selectByTxId(String txId);

    TxStepDO selectByTxIdAndIndex(String txId, int stepIndex);

    int updateStatusAndError(String txId, int stepIndex, String status, String lastError);

    int incRetryCount(String txId, int stepIndex);
}