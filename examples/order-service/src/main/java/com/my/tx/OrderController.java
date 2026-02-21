package com.my.tx;

import com.my.tx.common.DbIdempotencyService;
import com.my.tx.common.TxHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/order")
public class OrderController {

    private static final Map<String, String> ORDER = new ConcurrentHashMap<>();

    private final DbIdempotencyService idem;

    public OrderController(DbIdempotencyService idem) {
        this.idem = idem;
    }

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String idemKey = TxHeaders.key(req, "order:create");
        if (!idem.firstTime(idemKey)) {
            return Map.of("ok", true, "idempotent", true);
        }

        String orderId = String.valueOf(body.get("orderId"));
        ORDER.put(orderId, "CREATED");
        return Map.of("ok", true, "orderId", orderId, "status", "CREATED");
    }

    @PostMapping("/cancel")
    public Map<String, Object> cancel(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String idemKey = TxHeaders.key(req, "order:cancel");
        if (!idem.firstTime(idemKey)) {
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