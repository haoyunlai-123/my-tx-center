package com.my.tx;

import com.my.tx.common.TxHeaders;
import com.my.tx.common.TxIdempotency;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/order")
public class OrderController {

    // 用内存模拟“订单表”：orderId -> status
    private static final Map<String, String> ORDER = new ConcurrentHashMap<>();

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String idemKey = TxHeaders.key(req);
        if (!TxIdempotency.firstTime(idemKey)) {
            return Map.of("ok", true, "idempotent", true);
        }

        String orderId = String.valueOf(body.get("orderId"));
        ORDER.put(orderId, "CREATED");
        return Map.of("ok", true, "orderId", orderId, "status", "CREATED");
    }

    @PostMapping("/cancel")
    public Map<String, Object> cancel(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String idemKey = TxHeaders.key(req);
        if (!TxIdempotency.firstTime(idemKey)) {
            return Map.of("ok", true, "idempotent", true);
        }

        String orderId = String.valueOf(body.get("orderId"));
        ORDER.put(orderId, "CANCELED");
        return Map.of("ok", true, "orderId", orderId, "status", "CANCELED");
    }

    @GetMapping("/get")
    public Map<String, Object> get(@RequestParam("orderId") String orderId) {
        return Map.of("orderId", orderId, "status", ORDER.getOrDefault(orderId, "NONE"));
    }
}