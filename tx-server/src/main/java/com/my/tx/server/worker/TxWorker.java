package com.my.tx.server.worker;

import com.my.tx.common.enums.TxStatus;
import com.my.tx.server.engine.SagaEngine;
import com.my.tx.store.mapper.TxGlobalMapper;
import com.my.tx.store.model.TxGlobalDO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TxWorker {

    private final TxGlobalMapper txGlobalMapper;
    private final SagaEngine sagaEngine;

    public TxWorker(TxGlobalMapper txGlobalMapper, SagaEngine sagaEngine) {
        this.txGlobalMapper = txGlobalMapper;
        this.sagaEngine = sagaEngine;
    }

    @Scheduled(fixedDelay = 500) // 0.5s 扫一次，MVP 足够
    public void runOnce() {
        List<TxGlobalDO> list = txGlobalMapper.scanRunnable(
                List.of(TxStatus.RUNNING.name(), TxStatus.COMPENSATING.name()),
                LocalDateTime.now(),
                50
        );

        for (TxGlobalDO g : list) {
            // 乐观锁抢占，避免多实例重复推进（单实例也没坏处）
            int locked = txGlobalMapper.tryLock(g.getTxId(), g.getVersion());
            if (locked != 1) continue;

            try {
                sagaEngine.tick(g.getTxId());
            } catch (Exception e) {
                // 不要让 worker 挂
                e.printStackTrace();
            }
        }
    }
}