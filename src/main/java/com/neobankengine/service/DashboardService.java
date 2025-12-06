package com.neobankengine.service;

import com.neobankengine.dto.DashboardOverviewDto;
import com.neobankengine.dto.DailyPointDto;
import com.neobankengine.dto.MonthlySummaryDto;
import com.neobankengine.entity.Account;
import com.neobankengine.entity.Transaction;
import com.neobankengine.entity.User;
import com.neobankengine.repository.AccountRepository;
import com.neobankengine.repository.TransactionRepository;
import com.neobankengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Overview: total balance, total txns, credit/debit in last N days, small monthly list
     */
    @Transactional(readOnly = true)
    public DashboardOverviewDto getOverview(String userEmail, int lastNDays, int lastNMonths) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Account> accounts = accountRepository.findByUserId(user.getId());

        List<Long> accountIds = accounts.stream().map(Account::getAccountId).collect(Collectors.toList());

        // total balance
        double totalBalance = accounts.stream()
                .mapToDouble(a -> a.getBalance() == null ? 0.0 : a.getBalance())
                .sum();

        // time window for last N days
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(lastNDays);

        // transactions in window
        List<Transaction> recentTxs = accountIds.isEmpty() ? Collections.emptyList() :
                transactionRepository.findByAccountIdInAndTimestampBetween(accountIds, start, end);

        double totalCredit = recentTxs.stream()
                .filter(t -> "CREDIT".equalsIgnoreCase(t.getType()))
                .mapToDouble(t -> t.getAmount() == null ? 0.0 : t.getAmount())
                .sum();

        double totalDebit = recentTxs.stream()
                .filter(t -> "DEBIT".equalsIgnoreCase(t.getType()))
                .mapToDouble(t -> t.getAmount() == null ? 0.0 : t.getAmount())
                .sum();

        // monthly summaries (last N months)
        List<MonthlySummaryDto> months = buildMonthlySummaries(accountIds, lastNMonths);

        return new DashboardOverviewDto(totalBalance, accountIds.isEmpty() ? 0L : transactionRepository.countByAccountIdInAndTimestampBetween(accountIds, LocalDateTime.of(1970,1,1,0,0), end), totalCredit, totalDebit, months);
    }

    private List<MonthlySummaryDto> buildMonthlySummaries(List<Long> accountIds, int months) {
        List<MonthlySummaryDto> out = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = 0; i < months; i++) {
            YearMonth ym = current.minusMonths(i);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(LocalTime.MAX);

            List<Transaction> txs = accountIds.isEmpty() ? Collections.emptyList() :
                    transactionRepository.findByAccountIdInAndTimestampBetween(accountIds, start, end);

            double credit = txs.stream()
                    .filter(t -> "CREDIT".equalsIgnoreCase(t.getType()))
                    .mapToDouble(t -> t.getAmount() == null ? 0.0 : t.getAmount())
                    .sum();

            double debit = txs.stream()
                    .filter(t -> "DEBIT".equalsIgnoreCase(t.getType()))
                    .mapToDouble(t -> t.getAmount() == null ? 0.0 : t.getAmount())
                    .sum();

            out.add(new MonthlySummaryDto(ym.getYear(), ym.getMonthValue(), credit, debit));
        }

        // return newest-first (reverse order gives oldest-first), we want chronological descending -> keep as built
        return out;
    }

    /**
     * Daily series for last N days (useful for chart)
     */
    @Transactional(readOnly = true)
    public List<DailyPointDto> getDailySeries(String userEmail, int lastNDays) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Account> accounts = accountRepository.findByUserId(user.getId());
        List<Long> accountIds = accounts.stream().map(Account::getAccountId).collect(Collectors.toList());

        List<DailyPointDto> series = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < lastNDays; i++) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.atTime(LocalTime.MAX);

            List<Transaction> txs = accountIds.isEmpty() ? Collections.emptyList() :
                    transactionRepository.findByAccountIdInAndTimestampBetween(accountIds, start, end);

            double credit = txs.stream()
                    .filter(t -> "CREDIT".equalsIgnoreCase(t.getType()))
                    .mapToDouble(t -> t.getAmount() == null ? 0.0 : t.getAmount())
                    .sum();

            double debit = txs.stream()
                    .filter(t -> "DEBIT".equalsIgnoreCase(t.getType()))
                    .mapToDouble(t -> t.getAmount() == null ? 0.0 : t.getAmount())
                    .sum();

            series.add(new DailyPointDto(day, credit, debit));
        }

        return series; // newest-first (day=0 is today)
    }
}
