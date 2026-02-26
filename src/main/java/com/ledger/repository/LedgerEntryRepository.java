package com.ledger.repository;

import com.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    /**
     * Returns all ledger entries for an account, ordered chronologically.
     */
    List<LedgerEntry> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    /**
     * Calculates the current balance by summing all ledger entries for the account.
     * CREDIT entries increase the balance; DEBIT entries decrease it.
     * Uses a native query for reliable enum string comparison.
     */
    @Query(value = "SELECT COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount "
            + "WHEN entry_type = 'DEBIT' THEN -amount ELSE 0 END), 0) "
            + "FROM ledger_entries WHERE account_id = :accountId",
            nativeQuery = true)
    BigDecimal calculateBalance(@Param("accountId") UUID accountId);
}
