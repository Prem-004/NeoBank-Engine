package com.neobankengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BalanceResponse {
    private Long accountId;
    private Double balance;
}
