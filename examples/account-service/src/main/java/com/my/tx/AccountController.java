package com.my.tx;

import com.my.tx.common.DbIdempotencyService;
import com.my.tx.common.TxHeaders;
import com.my.tx.common.TxIdempotency;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/account")
public class AccountController {

    // user -> balance
    private static final Map<String, Integer> BAL = new ConcurrentHashMap<>();

    private final DbIdempotencyService idem;

    static {
        BAL.put("u1", 1000);
    }

    public AccountController(DbIdempotencyService idem) {
        this.idem = idem;
    }

    @PostMapping("/deduct")
    public Map<String, Object> deduct(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String idemKey = TxHeaders.key(req, "account:deduct");
        if (!idem.firstTime(idemKey)) {
            return Map.of("ok", true, "idempotent", true);
        }

        String user = String.valueOf(body.get("user"));
        int amount = Integer.parseInt(String.valueOf(body.get("amount")));

        Object fail = body.get("fail");
        if (fail != null && Boolean.parseBoolean(String.valueOf(fail))) {
            throw new RuntimeException("forced account deduct failure");
        }

        int left = BAL.getOrDefault(user, 0);
        if (left < amount) {
            throw new RuntimeException("insufficient balance");
        }
        BAL.put(user, left - amount);
        return Map.of("ok", true, "user", user, "balance", BAL.get(user));
    }

    @PostMapping("/refund")
    public Map<String, Object> refund(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String idemKey = TxHeaders.key(req, "account:refund");
        if (!idem.firstTime(idemKey)) {
            return Map.of("ok", true, "idempotent", true);
        }

        String user = String.valueOf(body.get("user"));
        int amount = Integer.parseInt(String.valueOf(body.get("amount")));

        int left = BAL.getOrDefault(user, 0);
        BAL.put(user, left + amount);
        return Map.of("ok", true, "user", user, "balance", BAL.get(user));
    }

    @GetMapping("/get")
    public Map<String, Object> get(@RequestParam("user") String user) {
        return Map.of("user", user, "balance", BAL.getOrDefault(user, 0));
    }
}