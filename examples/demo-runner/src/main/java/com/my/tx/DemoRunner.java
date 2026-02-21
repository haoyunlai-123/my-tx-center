package com.my.tx;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@SpringBootApplication
public class DemoRunner implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DemoRunner.class, args);
    }

    @Override
    public void run(String... args) {
        RestTemplate rt = new RestTemplate();

        // 改这里：是否强制让 stock/account 失败，触发补偿
        boolean failStock = false;
        boolean failAccount = true; // 先演示补偿链路

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("bizType", "order_create");
        req.put("bizKey", "order_1001");

        List<Map<String, Object>> steps = new ArrayList<>();

        steps.add(step("create-order",
                "http://127.0.0.1:9101/order/create", json(Map.of("orderId", "1001")),
                "http://127.0.0.1:9101/order/cancel", json(Map.of("orderId", "1001"))
        ));

        Map<String, Object> stockBody = new LinkedHashMap<>();
        stockBody.put("sku", "sku1");
        stockBody.put("n", 1);
        if (failStock) stockBody.put("fail", true);

        steps.add(step("deduct-stock",
                "http://127.0.0.1:9102/stock/deduct", json(stockBody),
                "http://127.0.0.1:9102/stock/compensate", json(Map.of("sku", "sku1", "n", 1))
        ));

        Map<String, Object> accBody = new LinkedHashMap<>();
        accBody.put("user", "u1");
        accBody.put("amount", 100);
        if (failAccount) accBody.put("fail", true);

        steps.add(step("deduct-balance",
                "http://127.0.0.1:9103/account/deduct", json(accBody),
                "http://127.0.0.1:9103/account/refund", json(Map.of("user", "u1", "amount", 100))
        ));

        req.put("steps", steps);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);

        String url = "http://127.0.0.1:9000/tx/start";
        ResponseEntity<String> resp = rt.postForEntity(url, entity, String.class);
        System.out.println("StartTx resp=" + resp.getBody());
    }

    private static Map<String, Object> step(String name,
                                            String actionUrl, String actionBody,
                                            String compUrl, String compBody) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("stepName", name);
        s.put("actionMethod", "POST");
        s.put("actionUrl", actionUrl);
        s.put("actionBody", actionBody);
        s.put("compensateMethod", "POST");
        s.put("compensateUrl", compUrl);
        s.put("compensateBody", compBody);
        s.put("retryMax", 3);
        s.put("timeoutMs", 3000);
        return s;
    }

    private static String json(Map<String, Object> m) {
        // 偷懒：手写 JSON（字段简单足够）。你也可以引 Jackson ObjectMapper。
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (var e : m.entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else sb.append("\"").append(String.valueOf(v)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }
}