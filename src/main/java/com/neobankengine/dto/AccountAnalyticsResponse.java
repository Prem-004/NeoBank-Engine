package com.neobankengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountAnalyticsResponse {

    private Long accountId;

    private Double totalCredits;   // sum of CREDIT amounts
    private Double totalDebits;    // sum of DEBIT amounts

    private Double netChange;      // totalCredits - totalDebits

    private Long creditCount;      // number of CREDIT txns
    private Long debitCount;       // number of DEBIT txns
}
