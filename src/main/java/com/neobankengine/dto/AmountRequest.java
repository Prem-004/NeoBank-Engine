package com.neobankengine.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AmountRequest {
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private Double amount;
}
