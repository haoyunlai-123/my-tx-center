package com.my.tx;

import com.my.tx.common.DbIdempotencyService;
import com.my.tx.common.TxHeaders;
import com.my.tx.common.TxIdempotency;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/stock")
public class StockController {

    // sku -> available
    private static final Map<String, Integer> STOCK = new ConcurrentHashMap<>();

    private final DbIdempotencyService idem;

    static {
        STOCK.put("sku1", 10);
    }

    public StockController(DbIdempotencyService idem) {
        this.idem = idem;
    }

    @PostMapping("/deduct")
    public Map<String, Object> deduct(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String idemKey = TxHeaders.key(req, "stock:deduct");
        if (!idem.firstTime(idemKey)) {
            return Map.of("ok", true, "idempotent", true);
        }

        String sku = String.valueOf(body.get("sku"));
        int n = Integer.parseInt(String.valueOf(body.get("n")));

        // 支持 demo：传 fail=true 强制失败
        Object fail = body.get("fail");
        if (fail != null && Boolean.parseBoolean(String.valueOf(fail))) {
            throw new RuntimeException("forced stock deduct failure");
        }

        int left = STOCK.getOrDefault(sku, 0);
        if (left < n) {
            throw new RuntimeException("insufficient stock");
        }
        STOCK.put(sku, left - n);
        return Map.of("ok", true, "sku", sku, "left", STOCK.get(sku));
    }

    @PostMapping("/compensate")
    public Map<String, Object> compensate(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String idemKey = TxHeaders.key(req, "stock:compensate");
        if (!idem.firstTime(idemKey)) {
            return Map.of("ok", true, "idempotent", true);
        }

        String sku = String.valueOf(body.get("sku"));
        int n = Integer.parseInt(String.valueOf(body.get("n")));

        int left = STOCK.getOrDefault(sku, 0);
        STOCK.put(sku, left + n);
        return Map.of("ok", true, "sku", sku, "left", STOCK.get(sku));
    }

    @GetMapping("/get")
    public Map<String, Object> get(@RequestParam("sku") String sku) {
        return Map.of("sku", sku, "left", STOCK.getOrDefault(sku, 0));
    }
}