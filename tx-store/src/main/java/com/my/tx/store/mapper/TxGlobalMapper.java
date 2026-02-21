package com.my.tx.store.mapper;

import com.my.tx.store.model.TxGlobalDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TxGlobalMapper {

    int insert(TxGlobalDO row);

    TxGlobalDO selectByTxId(@Param("txId") String txId);

    int updateStatusAndError(@Param("txId") String txId,
                             @Param("status") String status,
                             @Param("errorCode") String errorCode,
                             @Param("errorMsg") String errorMsg);

    int updateNextRunAt(@Param("txId") String txId, @Param("nextRunAt") LocalDateTime nextRunAt);

    /**
     * 乐观锁抢占：version+1，成功返回1，失败返回0
     */
    int tryLock(@Param("txId") String txId, @Param("version") long version);

    /**
     * 扫描待推进事务（worker 用，下一步会用到）
     */
    List<TxGlobalDO> scanRunnable(@Param("statuses") List<String> statuses,
                                  @Param("now") LocalDateTime now,
                                  @Param("limit") int limit);
}