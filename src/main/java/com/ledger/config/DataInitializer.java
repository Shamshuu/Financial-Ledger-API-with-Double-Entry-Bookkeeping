package com.ledger.config;

import com.ledger.entity.Account;
import com.ledger.enums.AccountStatus;
import com.ledger.enums.AccountType;
import com.ledger.repository.AccountRepository;
import com.ledger.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AccountRepository accountRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createSystemAccount();
        createImmutabilityTrigger();
    }

    /**
     * Creates the well-known system account if it does not already exist.
     * This account serves as the counterparty for deposits (debit side)
     * and withdrawals (credit side), maintaining double-entry integrity.
     */
    private void createSystemAccount() {
        if (accountRepository.findById(TransactionService.SYSTEM_ACCOUNT_ID).isEmpty()) {
            Account system = new Account();
            system.setId(TransactionService.SYSTEM_ACCOUNT_ID);
            system.setUserId("SYSTEM");
            system.setAccountType(AccountType.CHECKING);
            system.setCurrency("USD");
            system.setStatus(AccountStatus.ACTIVE);
            system.setCreatedAt(LocalDateTime.now());
            system.setUpdatedAt(LocalDateTime.now());
            accountRepository.save(system);
            log.info("System account created: {}", TransactionService.SYSTEM_ACCOUNT_ID);
        } else {
            log.info("System account already exists");
        }
    }

    /**
     * Installs a PostgreSQL trigger that prevents UPDATE and DELETE operations
     * on the ledger_entries table, enforcing immutability at the database level.
     * This complements the Hibernate @Immutable annotation for defence-in-depth.
     */
    private void createImmutabilityTrigger() {
        try {
            jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION prevent_ledger_modification()
                RETURNS TRIGGER AS $$
                BEGIN
                    RAISE EXCEPTION 'Ledger entries are immutable and cannot be modified or deleted';
                    RETURN NULL;
                END;
                $$ LANGUAGE plpgsql
                """);

            jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_trigger WHERE tgname = 'prevent_ledger_update'
                    ) THEN
                        CREATE TRIGGER prevent_ledger_update
                            BEFORE UPDATE OR DELETE ON ledger_entries
                            FOR EACH ROW
                            EXECUTE FUNCTION prevent_ledger_modification();
                    END IF;
                END $$
                """);

            log.info("Ledger immutability trigger installed");
        } catch (Exception e) {
            log.warn("Could not install immutability trigger (non-fatal): {}", e.getMessage());
        }
    }
}
