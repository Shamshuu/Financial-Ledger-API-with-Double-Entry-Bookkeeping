package com.ledger.exception;

import java.util.UUID;

public class AccountFrozenException extends RuntimeException {

    public AccountFrozenException(UUID accountId) {
        super("Account is not active (frozen or closed): " + accountId);
    }
}
