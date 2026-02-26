package com.ledger.service;

import com.ledger.dto.response.LedgerEntryResponse;
import com.ledger.entity.LedgerEntry;
import com.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for ledger entry queries and balance calculation.
 * Balance is always computed on-demand by summing all ledger entries,
 * ensuring it is always consistent with the transaction history.
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    /**
     * Calculates the current balance by summing all ledger entries for an account.
     * CREDIT entries add to the balance; DEBIT entries subtract from it.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateBalance(UUID accountId) {
        return ledgerEntryRepository.calculateBalance(accountId);
    }

    /**
     * Returns a chronological list of all ledger entries for the given account.
     */
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getLedgerEntries(UUID accountId) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtAsc(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private LedgerEntryResponse toResponse(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getAccountId(),
                entry.getTransactionId(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getCreatedAt()
        );
    }
}
