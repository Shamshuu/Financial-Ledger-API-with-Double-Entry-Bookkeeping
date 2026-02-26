package com.ledger.controller;

import com.ledger.dto.request.CreateAccountRequest;
import com.ledger.dto.response.AccountResponse;
import com.ledger.dto.response.LedgerEntryResponse;
import com.ledger.exception.AccountNotFoundException;
import com.ledger.repository.AccountRepository;
import com.ledger.service.AccountService;
import com.ledger.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final AccountRepository accountRepository;

    /**
     * POST /accounts — Create a new user account.
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /accounts/{accountId} — Retrieve account details with calculated balance.
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID accountId) {
        AccountResponse response = accountService.getAccount(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /accounts/{accountId}/ledger — Fetch chronological ledger entries for an account.
     */
    @GetMapping("/{accountId}/ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getLedger(@PathVariable UUID accountId) {
        // Verify the account exists first
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        List<LedgerEntryResponse> entries = ledgerService.getLedgerEntries(accountId);
        return ResponseEntity.ok(entries);
    }
}
