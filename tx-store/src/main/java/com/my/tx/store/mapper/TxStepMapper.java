package com.my.tx.store.mapper;

import com.my.tx.store.model.TxStepDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TxStepMapper {

    int insertBatch(@Param("list") List<TxStepDO> list);

    List<TxStepDO> selectByTxId(@Param("txId") String txId);

    TxStepDO selectByTxIdAndIndex(@Param("txId") String txId, @Param("stepIndex") int stepIndex);

    int updateStatusAndError(@Param("txId") String txId,
                             @Param("stepIndex") int stepIndex,
                             @Param("status") String status,
                             @Param("lastError") String lastError);

    int incRetryCount(@Param("txId") String txId, @Param("stepIndex") int stepIndex);
}