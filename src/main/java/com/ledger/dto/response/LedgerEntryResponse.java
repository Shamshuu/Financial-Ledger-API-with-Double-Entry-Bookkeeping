package com.ledger.dto.response;

import com.ledger.enums.EntryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryResponse {

    private UUID id;
    private UUID accountId;
    private UUID transactionId;
    private EntryType entryType;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
