package com.my.tx.store.mapper;

import com.my.tx.store.model.TxLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TxLogMapper {
    int insert(TxLogDO row);

    List<TxLogDO> selectByTxId(@Param("txId") String txId, @Param("limit") int limit);
}