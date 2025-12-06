package com.neobankengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class DailyPointDto {
    private LocalDate date;
    private Double totalCredit;
    private Double totalDebit;
}
