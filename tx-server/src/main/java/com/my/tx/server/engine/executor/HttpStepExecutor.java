package com.my.tx.server.engine.executor;

import com.my.tx.common.enums.TxPhase;
import com.my.tx.store.mapper.TxLogMapper;
import com.my.tx.store.model.TxLogDO;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 执行事务步骤
 */
@Component
public class HttpStepExecutor {

    private final RestTemplate restTemplate;
    private final TxLogMapper txLogMapper;

    public HttpStepExecutor(TxLogMapper txLogMapper) {
        this.restTemplate = new RestTemplate();
        this.txLogMapper = txLogMapper;
    }

    public ExecResult execute(String txId, int stepIndex, TxPhase phase,
                              String method, String url, String body, int timeoutMs) {
        long start = System.currentTimeMillis();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // 幂等键：业务服务可用它做去重（后面我们在 examples 里实现）
            headers.add("X-TX-ID", txId);
            headers.add("X-STEP-INDEX", String.valueOf(stepIndex));
            headers.add("X-PHASE", phase.name());

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            // 先简单用 RestTemplate，timeout 先不折腾（后面再升级为 HttpClient + timeouts）
            ResponseEntity<String> resp = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(method.toUpperCase()),
                    entity,
                    String.class
            );

            int cost = (int) (System.currentTimeMillis() - start);
            boolean ok = resp.getStatusCode().is2xxSuccessful();

            writeLog(txId, stepIndex, phase, ok, cost,
                    ok ? "OK" : ("HTTP " + resp.getStatusCode().value()));

            if (!ok) {
                return ExecResult.fail("HTTP_STATUS_" + resp.getStatusCode().value());
            }
            return ExecResult.ok();
        } catch (Exception e) {
            int cost = (int) (System.currentTimeMillis() - start);
            writeLog(txId, stepIndex, phase, false, cost, e.getClass().getSimpleName() + ": " + e.getMessage());
            return ExecResult.fail(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void writeLog(String txId, int stepIndex, TxPhase phase, boolean success, int costMs, String msg) {
        TxLogDO log = new TxLogDO();
        log.setTxId(txId);
        log.setStepIndex(stepIndex);
        log.setPhase(phase.name());
        log.setSuccess(success);
        log.setCostMs(costMs);
        log.setMessage(msg);
        txLogMapper.insert(log);
    }

    public static class ExecResult {
        private final boolean success;
        private final String error;

        private ExecResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }

        public static ExecResult ok() { return new ExecResult(true, null); }
        public static ExecResult fail(String err) { return new ExecResult(false, err); }

        public boolean isSuccess() { return success; }
        public String getError() { return error; }
    }
}