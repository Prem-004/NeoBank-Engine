package com.neobankengine.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransferRequest {

    @NotNull(message = "fromAccountId is required")
    private Long fromAccountId;

    @NotNull(message = "toAccountId is required")
    private Long toAccountId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be > 0")
    private Double amount;

    private String note; // optional reference text
}
