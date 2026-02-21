package com.my.tx.server.engine;

import com.my.tx.common.enums.StepStatus;
import com.my.tx.common.enums.TxPhase;
import com.my.tx.common.enums.TxStatus;
import com.my.tx.server.engine.executor.HttpStepExecutor;
import com.my.tx.store.mapper.TxGlobalMapper;
import com.my.tx.store.mapper.TxStepMapper;
import com.my.tx.store.model.TxGlobalDO;
import com.my.tx.store.model.TxStepDO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SagaEngine {

    private final TxGlobalMapper txGlobalMapper;
    private final TxStepMapper txStepMapper;
    private final HttpStepExecutor httpStepExecutor;

    public SagaEngine(TxGlobalMapper txGlobalMapper,
                      TxStepMapper txStepMapper,
                      HttpStepExecutor httpStepExecutor) {
        this.txGlobalMapper = txGlobalMapper;
        this.txStepMapper = txStepMapper;
        this.httpStepExecutor = httpStepExecutor;
    }

    /**
     * 推进一次：要么执行一个 ACTION step，要么执行一个 COMPENSATE step
     */
    public void tick(String txId) {
        TxGlobalDO g = txGlobalMapper.selectByTxId(txId);
        if (g == null) return;

        TxStatus status = TxStatus.valueOf(g.getStatus());

        if (status == TxStatus.RUNNING) {
            doActionTick(g);
            return;
        }
        if (status == TxStatus.COMPENSATING) {
            doCompensateTick(g);
        }
    }

    private void doActionTick(TxGlobalDO g) {
        List<TxStepDO> steps = txStepMapper.selectByTxId(g.getTxId());
        int idx = g.getCurrentStep();

        if (idx >= steps.size()) {
            txGlobalMapper.updateStatusAndError(g.getTxId(), TxStatus.SUCCEEDED.name(), null, null);
            return;
        }

        TxStepDO step = steps.get(idx);

        // 只处理 PENDING / ACTION_FAILED（失败可重试）
        StepStatus stepStatus = StepStatus.valueOf(step.getStatus());
        if (!(stepStatus == StepStatus.PENDING || stepStatus == StepStatus.ACTION_FAILED)) {
            // 如果状态异常，直接推进指针避免卡死（也可以改为 FAILED）
            txGlobalMapper.updateCurrentStep(g.getTxId(), idx + 1);
            return;
        }

        HttpStepExecutor.ExecResult r = httpStepExecutor.execute(
                g.getTxId(),
                idx,
                TxPhase.ACTION,
                step.getActionMethod(),
                step.getActionUrl(),
                step.getActionBody(),
                step.getTimeoutMs()
        );

        if (r.isSuccess()) {
            txStepMapper.updateStatusAndError(g.getTxId(), idx, StepStatus.ACTION_DONE.name(), null);
            txGlobalMapper.updateCurrentStep(g.getTxId(), idx + 1);
            txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now()); // 立刻推进下一步
            return;
        }

        // 失败：记录 step 失败并重试计数；超过 retryMax -> 进入补偿
        txStepMapper.updateStatusAndError(g.getTxId(), idx, StepStatus.ACTION_FAILED.name(), r.getError());
        txStepMapper.incRetryCount(g.getTxId(), idx);

        // 简单做法：用 retry_count < retry_max 判断是否继续重试
        TxStepDO fresh = txStepMapper.selectByTxIdAndIndex(g.getTxId(), idx);
        if (fresh.getRetryCount() < fresh.getRetryMax()) {
            // 退避：先固定 2s（下一轮我们改成指数退避）
            txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now().plusSeconds(2));
            return;
        }

        // 超过最大重试：切换补偿状态，从已完成的最后一步开始回滚
        txGlobalMapper.updateStatusAndError(g.getTxId(), TxStatus.COMPENSATING.name(), "ACTION_FAILED", r.getError());
        txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now());
    }

    private void doCompensateTick(TxGlobalDO g) {
        List<TxStepDO> steps = txStepMapper.selectByTxId(g.getTxId());

        // current_step 指向“下一步要执行的 action”，已完成 action 的最后一步是 current_step-1
        int lastDone = g.getCurrentStep() - 1;

        if (lastDone < 0) {
            // 没有任何 action 成功过
            txGlobalMapper.updateStatusAndError(g.getTxId(), TxStatus.COMPENSATED.name(), null, null);
            return;
        }

        TxStepDO step = steps.get(lastDone);
        StepStatus st = StepStatus.valueOf(step.getStatus());

        // 只对 ACTION_DONE 的步骤执行补偿；已补偿的直接跳过
        if (st == StepStatus.COMPENSATE_DONE) {
            txGlobalMapper.updateCurrentStep(g.getTxId(), lastDone); // 指针回退
            txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now());
            return;
        }
        if (st != StepStatus.ACTION_DONE && st != StepStatus.COMPENSATE_FAILED) {
            // 状态异常：跳过回退（保守策略）
            txGlobalMapper.updateCurrentStep(g.getTxId(), lastDone);
            return;
        }

        HttpStepExecutor.ExecResult r = httpStepExecutor.execute(
                g.getTxId(),
                lastDone,
                TxPhase.COMPENSATE,
                step.getCompensateMethod(),
                step.getCompensateUrl(),
                step.getCompensateBody(),
                step.getTimeoutMs()
        );

        if (r.isSuccess()) {
            txStepMapper.updateStatusAndError(g.getTxId(), lastDone, StepStatus.COMPENSATE_DONE.name(), null);
            txGlobalMapper.updateCurrentStep(g.getTxId(), lastDone); // 继续回退补偿上一步
            txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now());
            return;
        }

        // 补偿失败：记状态 + 下次重试
        txStepMapper.updateStatusAndError(g.getTxId(), lastDone, StepStatus.COMPENSATE_FAILED.name(), r.getError());
        // 这里也可以复用 retry_count/retry_max（简单起见先不分 action/compensate 的计数）
        txStepMapper.incRetryCount(g.getTxId(), lastDone);

        TxStepDO fresh = txStepMapper.selectByTxIdAndIndex(g.getTxId(), lastDone);
        if (fresh.getRetryCount() < fresh.getRetryMax()) {
            txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now().plusSeconds(2));
            return;
        }

        // 补偿也失败到顶：事务 FAILED，需要人工介入
        txGlobalMapper.updateStatusAndError(g.getTxId(), TxStatus.FAILED.name(), "COMPENSATE_FAILED", r.getError());
    }
}