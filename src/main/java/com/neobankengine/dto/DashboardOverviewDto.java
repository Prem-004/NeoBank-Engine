package com.neobankengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class DashboardOverviewDto {
    private Double totalBalance;
    private Long totalTransactions;
    private Double totalCreditLastNDays;
    private Double totalDebitLastNDays;
    private List<MonthlySummaryDto> lastMonths; // optional small summary list
}
