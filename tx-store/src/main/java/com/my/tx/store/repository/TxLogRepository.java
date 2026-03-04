package com.my.tx.store.repository;

import com.my.tx.store.model.TxLogDO;

import java.util.List;

public interface TxLogRepository {

    int insert(TxLogDO row);

    List<TxLogDO> selectByTxId(String txId, int limit);
}