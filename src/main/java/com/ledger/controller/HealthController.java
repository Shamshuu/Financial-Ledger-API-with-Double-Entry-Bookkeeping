package com.ledger.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "Financial Ledger API",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "endpoints", Map.of(
                        "POST /accounts", "Create a new account",
                        "GET /accounts/{id}", "Get account details with balance",
                        "GET /accounts/{id}/ledger", "Get ledger entries",
                        "POST /transfers", "Transfer between accounts",
                        "POST /deposits", "Deposit into an account",
                        "POST /withdrawals", "Withdraw from an account"
                )
        ));
    }
}
