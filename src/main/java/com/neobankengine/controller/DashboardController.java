package com.neobankengine.controller;

import com.neobankengine.dto.DashboardOverviewDto;
import com.neobankengine.dto.DailyPointDto;
import com.neobankengine.dto.MonthlySummaryDto;
import com.neobankengine.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewDto> overview(
            @RequestParam(value = "days", defaultValue = "30") int days,
            @RequestParam(value = "months", defaultValue = "6") int months) {

        String email = currentUserEmail();
        DashboardOverviewDto dto = dashboardService.getOverview(email, days, months);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlySummaryDto>> monthly(
            @RequestParam(value = "months", defaultValue = "6") int months) {

        String email = currentUserEmail();
        List<MonthlySummaryDto> list = dashboardService.getOverview(email, 30, months).getLastMonths();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailyPointDto>> daily(
            @RequestParam(value = "days", defaultValue = "30") int days) {

        String email = currentUserEmail();
        List<DailyPointDto> list = dashboardService.getDailySeries(email, days);
        return ResponseEntity.ok(list);
    }
}
