package com.neobankengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlySummaryDto {
    private int year;
    private int month; // 1..12
    private Double totalCredit;
    private Double totalDebit;
}
