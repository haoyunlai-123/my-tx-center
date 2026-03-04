package com.my.tx.store.repository.mybatis;

import com.my.tx.store.mapper.TxGlobalMapper;
import com.my.tx.store.model.TxGlobalDO;
import com.my.tx.store.repository.TxGlobalRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MybatisTxGlobalRepository implements TxGlobalRepository {

    private final TxGlobalMapper mapper;

    public MybatisTxGlobalRepository(TxGlobalMapper mapper) {
        this.mapper = mapper;
    }

    @Override public TxGlobalDO selectByTxId(String txId) { return mapper.selectByTxId(txId); }
    @Override public int insert(TxGlobalDO row) { return mapper.insert(row); }
    @Override public int updateStatusAndError(String txId, String status, String errorCode, String errorMsg) {
        return mapper.updateStatusAndError(txId, status, errorCode, errorMsg);
    }
    @Override public int updateNextRunAt(String txId, LocalDateTime t) { return mapper.updateNextRunAt(txId, t); }
    @Override public int updateCurrentStep(String txId, int currentStep) { return mapper.updateCurrentStep(txId, currentStep); }
    @Override public int tryLock(String txId, long expectVersion) { return mapper.tryLock(txId, expectVersion); }

    @Override
    public List<TxGlobalDO> scanRunnable(List<String> statuses, LocalDateTime now, int limit) {
        return mapper.scanRunnable(statuses, now, limit);
    }
}