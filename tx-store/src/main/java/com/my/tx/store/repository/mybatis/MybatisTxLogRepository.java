package com.my.tx.store.repository.mybatis;

import com.my.tx.store.mapper.TxLogMapper;
import com.my.tx.store.model.TxLogDO;
import com.my.tx.store.repository.TxLogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisTxLogRepository implements TxLogRepository {

    private final TxLogMapper mapper;

    public MybatisTxLogRepository(TxLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override public int insert(TxLogDO row) { return mapper.insert(row); }
    @Override public List<TxLogDO> selectByTxId(String txId, int limit) { return mapper.selectByTxId(txId, limit); }
}