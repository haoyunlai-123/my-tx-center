package com.my.tx.store.mapper;

import com.my.tx.store.model.TxLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TxLogMapper {
    int insert(TxLogDO row);
}