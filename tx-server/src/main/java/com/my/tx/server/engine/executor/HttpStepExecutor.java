package com.my.tx.server.engine.executor;

import com.my.tx.common.enums.TxPhase;
import com.my.tx.store.mapper.TxLogMapper;
import com.my.tx.store.model.TxLogDO;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class HttpStepExecutor {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient baseClient;
    private final TxLogMapper txLogMapper;

    public HttpStepExecutor(OkHttpClient okHttpClient, TxLogMapper txLogMapper) {
        this.baseClient = okHttpClient;
        this.txLogMapper = txLogMapper;
    }

    public ExecResult execute(String txId, int stepIndex, TxPhase phase,
                              String method, String url, String body, int timeoutMs) {
        long start = System.currentTimeMillis();

        OkHttpClient client = baseClient.newBuilder()
                .callTimeout(Duration.ofMillis(Math.max(1, timeoutMs)))
                .build();

        RequestBody rb = RequestBody.create(body == null ? "" : body, JSON);

        Request req = new Request.Builder()
                .url(url)
                .method(method == null ? "POST" : method.toUpperCase(), rb)
                .header("Content-Type", "application/json")
                .header("X-TX-ID", txId)
                .header("X-STEP-INDEX", String.valueOf(stepIndex))
                .header("X-PHASE", phase.name())
                .build();

        try (Response resp = client.newCall(req).execute()) {
            int cost = (int) (System.currentTimeMillis() - start);
            int code = resp.code();

            String respBody = resp.body() == null ? "" : resp.body().string();
            boolean ok = code >= 200 && code < 300;

            writeLog(txId, stepIndex, phase, ok, cost,
                    ok ? ("OK " + code) : ("HTTP_" + code + " " + shrink(respBody)));

            if (ok) return ExecResult.ok();

            // 错误分类：4xx 通常不可重试；5xx 可重试
            boolean retryable = (code >= 500 && code <= 599);
            return ExecResult.fail("HTTP_" + code, retryable);

        } catch (IOException e) {
            int cost = (int) (System.currentTimeMillis() - start);
            // 网络/超时：可重试
            writeLog(txId, stepIndex, phase, false, cost,
                    "IO: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return ExecResult.fail("IO_" + e.getClass().getSimpleName(), true);
        } catch (Exception e) {
            int cost = (int) (System.currentTimeMillis() - start);
            // 其他异常：默认不可重试（也可按需改）
            writeLog(txId, stepIndex, phase, false, cost,
                    "EX: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return ExecResult.fail("EX_" + e.getClass().getSimpleName(), false);
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

    private static String shrink(String s) {
        if (s == null) return "";
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        if (b.length <= 200) return s;
        return new String(b, 0, 200, StandardCharsets.UTF_8) + "...";
    }

    public static class ExecResult {
        private final boolean success;
        private final String error;
        private final boolean retryable;

        private ExecResult(boolean success, String error, boolean retryable) {
            this.success = success;
            this.error = error;
            this.retryable = retryable;
        }

        public static ExecResult ok() { return new ExecResult(true, null, false); }
        public static ExecResult fail(String err, boolean retryable) { return new ExecResult(false, err, retryable); }

        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public boolean isRetryable() { return retryable; }
    }
}