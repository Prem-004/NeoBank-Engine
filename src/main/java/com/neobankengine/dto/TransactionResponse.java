package com.neobankengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransactionResponse
{
    private Long transactionId;
    private Long accountId;
    private String type;          // CREDIT / DEBIT
    private Double amount;
    private LocalDateTime timestamp;
    private String referenceText;
}
