package com.ledger.service;

import com.ledger.dto.request.CreateAccountRequest;
import com.ledger.dto.response.AccountResponse;
import com.ledger.entity.Account;
import com.ledger.enums.AccountStatus;
import com.ledger.exception.AccountNotFoundException;
import com.ledger.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerService ledgerService;

    /**
     * Creates a new account with ACTIVE status and zero balance.
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account();
        account.setUserId(request.getUserId());
        account.setAccountType(request.getAccountType());
        account.setCurrency(request.getCurrency().toUpperCase());
        account.setStatus(AccountStatus.ACTIVE);

        account = accountRepository.save(account);

        // New account always has zero balance
        return toResponse(account, BigDecimal.ZERO);
    }

    /**
     * Retrieves an account by ID with its current balance calculated from ledger entries.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        BigDecimal balance = ledgerService.calculateBalance(accountId);
        return toResponse(account, balance);
    }

    private AccountResponse toResponse(Account account, BigDecimal balance) {
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getAccountType(),
                account.getCurrency(),
                account.getStatus(),
                balance,
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
