package com.ledger.service;

import com.ledger.dto.request.DepositRequest;
import com.ledger.dto.request.TransferRequest;
import com.ledger.dto.request.WithdrawalRequest;
import com.ledger.dto.response.TransactionResponse;
import com.ledger.entity.Account;
import com.ledger.entity.LedgerEntry;
import com.ledger.entity.Transaction;
import com.ledger.enums.*;
import com.ledger.exception.AccountFrozenException;
import com.ledger.exception.AccountNotFoundException;
import com.ledger.exception.InsufficientFundsException;
import com.ledger.exception.InvalidOperationException;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.LedgerEntryRepository;
import com.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Core service that orchestrates all financial operations (transfers, deposits, withdrawals).
 * <p>
 * Every operation follows the double-entry bookkeeping principle: each financial
 * movement generates exactly two balanced ledger entries (a debit and a credit)
 * whose amounts sum to zero.
 * <p>
 * Concurrency safety is achieved through pessimistic locking (SELECT ... FOR UPDATE)
 * on the involved account rows, combined with READ_COMMITTED isolation to ensure
 * the balance check always reflects the latest committed state.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    /**
     * Well-known UUID for the system/external account used in deposits and withdrawals
     * to maintain double-entry bookkeeping integrity.
     */
    public static final UUID SYSTEM_ACCOUNT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    // -------------------------------------------------------------------------
    // Transfer
    // -------------------------------------------------------------------------

    /**
     * Executes a transfer between two internal accounts.
     * <p>
     * Locks both accounts in a consistent order (by UUID) to prevent deadlocks,
     * validates balances, creates a DEBIT entry on the source and a CREDIT entry
     * on the destination, and performs a post-entry balance verification.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse executeTransfer(TransferRequest request) {
        validateTransferRequest(request);

        // Lock accounts in consistent UUID order to prevent deadlocks
        UUID id1 = request.getSourceAccountId();
        UUID id2 = request.getDestinationAccountId();
        Account source, destination;

        if (id1.compareTo(id2) < 0) {
            Account first = lockAccount(id1);
            Account second = lockAccount(id2);
            source = first;
            destination = second;
        } else {
            Account first = lockAccount(id2);
            Account second = lockAccount(id1);
            source = second;
            destination = first;
        }

        validateAccountActive(source);
        validateAccountActive(destination);
        validateCurrencyMatch(source, destination);

        // Pre-entry balance check
        BigDecimal sourceBalance = ledgerEntryRepository.calculateBalance(source.getId());
        if (sourceBalance.compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(source.getId(), sourceBalance, request.getAmount());
        }

        // Create transaction record
        Transaction tx = buildTransaction(
                TransactionType.TRANSFER,
                source.getId(),
                destination.getId(),
                request.getAmount(),
                source.getCurrency(),
                request.getDescription() != null ? request.getDescription() : "Transfer"
        );
        tx = transactionRepository.save(tx);

        // Create balanced ledger entries (debit source, credit destination)
        persistLedgerEntry(source.getId(), tx.getId(), EntryType.DEBIT, request.getAmount());
        persistLedgerEntry(destination.getId(), tx.getId(), EntryType.CREDIT, request.getAmount());

        // Post-entry balance verification (belt-and-suspenders)
        BigDecimal newBalance = ledgerEntryRepository.calculateBalance(source.getId());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(source.getId(), newBalance, request.getAmount());
        }

        // Mark completed
        tx.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(tx);

        log.info("Transfer completed: {} -> {}, amount={} {}",
                source.getId(), destination.getId(), request.getAmount(), source.getCurrency());
        return toResponse(tx);
    }

    // -------------------------------------------------------------------------
    // Deposit
    // -------------------------------------------------------------------------

    /**
     * Simulates a deposit into an account.
     * <p>
     * Uses the system account as the debit side to maintain double-entry integrity.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse executeDeposit(DepositRequest request) {
        Account target = lockAccount(request.getAccountId());
        validateAccountActive(target);

        Transaction tx = buildTransaction(
                TransactionType.DEPOSIT,
                SYSTEM_ACCOUNT_ID,
                target.getId(),
                request.getAmount(),
                target.getCurrency(),
                request.getDescription() != null ? request.getDescription() : "Deposit"
        );
        tx = transactionRepository.save(tx);

        // Double-entry: debit system account, credit target account
        persistLedgerEntry(SYSTEM_ACCOUNT_ID, tx.getId(), EntryType.DEBIT, request.getAmount());
        persistLedgerEntry(target.getId(), tx.getId(), EntryType.CREDIT, request.getAmount());

        tx.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(tx);

        log.info("Deposit completed: account={}, amount={} {}",
                target.getId(), request.getAmount(), target.getCurrency());
        return toResponse(tx);
    }

    // -------------------------------------------------------------------------
    // Withdrawal
    // -------------------------------------------------------------------------

    /**
     * Simulates a withdrawal from an account.
     * <p>
     * Validates that the account has sufficient funds before proceeding.
     * Uses the system account as the credit side to maintain double-entry integrity.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse executeWithdrawal(WithdrawalRequest request) {
        Account source = lockAccount(request.getAccountId());
        validateAccountActive(source);

        // Pre-entry balance check
        BigDecimal balance = ledgerEntryRepository.calculateBalance(source.getId());
        if (balance.compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(source.getId(), balance, request.getAmount());
        }

        Transaction tx = buildTransaction(
                TransactionType.WITHDRAWAL,
                source.getId(),
                SYSTEM_ACCOUNT_ID,
                request.getAmount(),
                source.getCurrency(),
                request.getDescription() != null ? request.getDescription() : "Withdrawal"
        );
        tx = transactionRepository.save(tx);

        // Double-entry: debit source account, credit system account
        persistLedgerEntry(source.getId(), tx.getId(), EntryType.DEBIT, request.getAmount());
        persistLedgerEntry(SYSTEM_ACCOUNT_ID, tx.getId(), EntryType.CREDIT, request.getAmount());

        // Post-entry balance verification
        BigDecimal newBalance = ledgerEntryRepository.calculateBalance(source.getId());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(source.getId(), newBalance, request.getAmount());
        }

        tx.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(tx);

        log.info("Withdrawal completed: account={}, amount={} {}",
                source.getId(), request.getAmount(), source.getCurrency());
        return toResponse(tx);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Account lockAccount(UUID accountId) {
        return accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private void validateAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(account.getId());
        }
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new InvalidOperationException("Cannot transfer to the same account");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Transfer amount must be positive");
        }
    }

    private void validateCurrencyMatch(Account source, Account destination) {
        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new InvalidOperationException(
                    "Currency mismatch: source=" + source.getCurrency()
                            + ", destination=" + destination.getCurrency());
        }
    }

    private Transaction buildTransaction(TransactionType type, UUID sourceId, UUID destId,
                                          BigDecimal amount, String currency, String description) {
        Transaction tx = new Transaction();
        tx.setType(type);
        tx.setSourceAccountId(sourceId);
        tx.setDestinationAccountId(destId);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setStatus(TransactionStatus.PENDING);
        tx.setDescription(description);
        return tx;
    }

    private void persistLedgerEntry(UUID accountId, UUID transactionId,
                                     EntryType type, BigDecimal amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setAccountId(accountId);
        entry.setTransactionId(transactionId);
        entry.setEntryType(type);
        entry.setAmount(amount);
        ledgerEntryRepository.save(entry);
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getType(),
                tx.getSourceAccountId(),
                tx.getDestinationAccountId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus(),
                tx.getDescription(),
                tx.getCreatedAt(),
                tx.getUpdatedAt()
        );
    }
}
