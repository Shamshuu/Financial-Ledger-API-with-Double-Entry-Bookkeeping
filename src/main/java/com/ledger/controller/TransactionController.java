package com.ledger.controller;

import com.ledger.dto.request.DepositRequest;
import com.ledger.dto.request.TransferRequest;
import com.ledger.dto.request.WithdrawalRequest;
import com.ledger.dto.response.TransactionResponse;
import com.ledger.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /transfers — Execute a financial transfer between two internal accounts.
     */
    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.executeTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     */
    @PostMapping("/deposits")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request) {
        TransactionResponse response = transactionService.executeDeposit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawalRequest request) {
        TransactionResponse response = transactionService.executeWithdrawal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
