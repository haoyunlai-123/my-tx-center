package com.my.tx.store.repository.mybatis;

import com.my.tx.store.mapper.TxStepMapper;
import com.my.tx.store.model.TxStepDO;
import com.my.tx.store.repository.TxStepRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisTxStepRepository implements TxStepRepository {

    private final TxStepMapper mapper;

    public MybatisTxStepRepository(TxStepMapper mapper) {
        this.mapper = mapper;
    }

    @Override public int batchInsert(List<TxStepDO> rows) { return mapper.insertBatch(rows); }
    @Override public List<TxStepDO> selectByTxId(String txId) { return mapper.selectByTxId(txId); }
    @Override public TxStepDO selectByTxIdAndIndex(String txId, int stepIndex) { return mapper.selectByTxIdAndIndex(txId, stepIndex); }

    @Override
    public int updateStatusAndError(String txId, int stepIndex, String status, String lastError) {
        return mapper.updateStatusAndError(txId, stepIndex, status, lastError);
    }

    @Override public int incRetryCount(String txId, int stepIndex) { return mapper.incRetryCount(txId, stepIndex); }
}