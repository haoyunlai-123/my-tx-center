package com.my.tx.store.repository;

import com.my.tx.store.model.TxGlobalDO;

import java.time.LocalDateTime;
import java.util.List;

public interface TxGlobalRepository {

    TxGlobalDO selectByTxId(String txId);

    int insert(TxGlobalDO row);

    int updateStatusAndError(String txId, String status, String errorCode, String errorMsg);

    int updateNextRunAt(String txId, LocalDateTime t);

    int updateCurrentStep(String txId, int currentStep);

    /**
     * 用于多 worker 抢占：基于 version 乐观锁抢锁
     * @return 1=抢锁成功；0=失败
     */
    int tryLock(String txId, long expectVersion);

    /**
     * 扫描需要执行的事务（next_run_at <= now 且 status in (...)）
     */
    List<TxGlobalDO> scanRunnable(List<String> statuses, LocalDateTime now, int limit);
}