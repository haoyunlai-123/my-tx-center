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

        // 失败：记录 step 失败并重试计数
        txStepMapper.updateStatusAndError(g.getTxId(), idx, StepStatus.ACTION_FAILED.name(), r.getError());
        txStepMapper.incRetryCount(g.getTxId(), idx);

        TxStepDO fresh = txStepMapper.selectByTxIdAndIndex(g.getTxId(), idx);

        //  不可重试：直接补偿
        if (!r.isRetryable()) {
            txGlobalMapper.updateStatusAndError(g.getTxId(), TxStatus.COMPENSATING.name(), "NON_RETRYABLE", r.getError());
            txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now());
            return;
        }

        //  可重试：没超过 retryMax 就指数退避
        if (fresh.getRetryCount() < fresh.getRetryMax()) {
            long base = 2L; // 2s 起步
            long delay = (long) (base * Math.pow(2, Math.max(0, fresh.getRetryCount() - 1))); // 2,4,8...
            delay = Math.min(delay, 30L); // 上限 30s
            txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now().plusSeconds(delay));
            return;
        }

        // 超过最大重试：进入补偿
        txGlobalMapper.updateStatusAndError(g.getTxId(), TxStatus.COMPENSATING.name(), "ACTION_FAILED", r.getError());
        txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now());
    }

    private void doCompensateTick(TxGlobalDO g) {
        List<TxStepDO> steps = txStepMapper.selectByTxId(g.getTxId());

        // current_step 指向“下一步要执行的 action”，已完成 action 的最后一步是 current_step - 1
        int lastDone = g.getCurrentStep() - 1;

        if (lastDone < 0) {
            // 没有任何 action 成功过
            txGlobalMapper.updateStatusAndError(g.getTxId(), TxStatus.COMPENSATED.name(), null, null);
            return;
        }

        if (lastDone >= steps.size()) {
            // 防御：指针异常，直接标失败或回到边界
            txGlobalMapper.updateStatusAndError(g.getTxId(), TxStatus.FAILED.name(), "BAD_POINTER",
                    "currentStep out of range: " + g.getCurrentStep());
            return;
        }

        TxStepDO step = steps.get(lastDone);
        StepStatus st = StepStatus.valueOf(step.getStatus());

        // 已补偿：继续回退补偿上一步
        if (st == StepStatus.COMPENSATE_DONE) {
            txGlobalMapper.updateCurrentStep(g.getTxId(), lastDone);
            txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now());
            return;
        }

        // 只有 ACTION_DONE / COMPENSATE_FAILED 才会进入“执行补偿”
        if (st != StepStatus.ACTION_DONE && st != StepStatus.COMPENSATE_FAILED) {
            // 状态异常：保守跳过（也可以改成直接 FAILED 更严格）
            txGlobalMapper.updateCurrentStep(g.getTxId(), lastDone);
            txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now());
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
            // 继续回退补偿上一步：current_step 变为 lastDone（下次 lastDone 会变成 lastDone-1）
            txGlobalMapper.updateCurrentStep(g.getTxId(), lastDone);
            txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now());
            return;
        }

        // ===== 失败处理：写 step 状态 + retry_count =====
        txStepMapper.updateStatusAndError(g.getTxId(), lastDone, StepStatus.COMPENSATE_FAILED.name(), r.getError());
        txStepMapper.incRetryCount(g.getTxId(), lastDone);

        // 重新读取 retry_count/retry_max（避免并发下读旧值）
        TxStepDO fresh = txStepMapper.selectByTxIdAndIndex(g.getTxId(), lastDone);

        // 不可重试：直接 FAILED（补偿失败人工介入）
        if (!r.isRetryable()) {
            txGlobalMapper.updateStatusAndError(
                    g.getTxId(),
                    TxStatus.FAILED.name(),
                    "NON_RETRYABLE_COMPENSATE",
                    r.getError()
            );
            return;
        }

        // 可重试且未超过最大次数：指数退避
        if (fresh.getRetryCount() < fresh.getRetryMax()) {
            long delay = calcBackoffSeconds(fresh.getRetryCount());
            txGlobalMapper.updateNextRunAt(g.getTxId(), LocalDateTime.now().plusSeconds(delay));
            return;
        }

        // 超过最大重试：FAILED
        txGlobalMapper.updateStatusAndError(g.getTxId(), TxStatus.FAILED.name(), "COMPENSATE_FAILED", r.getError());
    }

    /**
     * retryCount 从 1 开始：2,4,8... 上限 30s
     */
    private long calcBackoffSeconds(int retryCount) {
        long base = 2L;
        long exp = Math.max(0, retryCount - 1); // retry=1 -> 2s
        long delay = (long) (base * Math.pow(2, exp));
        return Math.min(delay, 30L);
    }
}