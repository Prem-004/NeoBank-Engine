package com.neobankengine.service;

import com.neobankengine.dto.TransactionResponse;
import com.neobankengine.dto.AccountAnalyticsResponse;
import com.neobankengine.entity.Account;
import com.neobankengine.entity.Transaction;
import com.neobankengine.entity.User;
import com.neobankengine.repository.AccountRepository;
import com.neobankengine.repository.TransactionRepository;
import com.neobankengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Get paginated transaction history for an account
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(Long accountId, String userEmail, int page, int size) {

        // Verify account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found for this user."));

        // Verify user exists and is the owner
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found. Please log in again."));

        if (!account.getUserId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to view transactions for this account.");
        }

        // Pageable: Sort newest first
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        // Fetch paginated data
        Page<Transaction> txPage =
                transactionRepository.findByAccountId(accountId, pageable);

        // Convert entity → DTO using map
        return txPage.map(t -> new TransactionResponse(
                t.getTransactionId(),
                t.getAccountId(),
                t.getType(),
                t.getAmount(),
                t.getTimestamp(),
                t.getReferenceText()
        ));
    }

    /**
     * Get last N transactions (mini statement)
     */
    @Transactional(readOnly = true)
    public java.util.List<TransactionResponse> getLastNTransactions(Long accountId, String userEmail, int limit) {

        // Verify account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Verify user exists and own account
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!account.getUserId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        PageRequest pageable = PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        return transactionRepository.findByAccountId(accountId, pageable)
                .map(t -> new TransactionResponse(
                        t.getTransactionId(),
                        t.getAccountId(),
                        t.getType(),
                        t.getAmount(),
                        t.getTimestamp(),
                        t.getReferenceText()
                ))
                .getContent();
    }

    /**
     * Get transactions for statement export with optional date range (from/to are LocalDate)
     * Returns a list ordered by timestamp DESC (newest first)
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsForStatement(Long accountId, String userEmail, LocalDate from, LocalDate to) {
        // Verify account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Verify user exists and is the owner
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!account.getUserId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        LocalDateTime fromDt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toDt = (to == null) ? null : to.atTime(LocalTime.MAX);

        List<Transaction> txs;

        if (fromDt == null && toDt == null) {
            // all transactions, newest first
            txs = transactionRepository.findByAccountIdOrderByTimestampDesc(accountId);
        } else if (fromDt != null && toDt != null) {
            txs = transactionRepository.findByAccountIdAndTimestampBetweenOrderByTimestampDesc(accountId, fromDt, toDt);
        } else if (fromDt != null) {
            txs = transactionRepository.findByAccountIdAndTimestampAfterOrderByTimestampDesc(accountId, fromDt);
        } else { // toDt != null
            txs = transactionRepository.findByAccountIdAndTimestampBeforeOrderByTimestampDesc(accountId, toDt);
        }

        return txs.stream()
                .map(t -> new TransactionResponse(
                        t.getTransactionId(),
                        t.getAccountId(),
                        t.getType(),
                        t.getAmount(),
                        t.getTimestamp(),
                        t.getReferenceText()
                ))
                .collect(Collectors.toList());
    }
    /**
     * Simple analytics for an account over an optional date range.
     * Uses the same ownership checks as getTransactionsForStatement.
     */
    @Transactional(readOnly = true)
    public AccountAnalyticsResponse getAnalytics(
            Long accountId,
            String userEmail,
            LocalDate from,
            LocalDate to
    ) {
        // Reuse existing statement logic (it already checks account & user)
        var txs = getTransactionsForStatement(accountId, userEmail, from, to);

        double totalCredits = 0.0;
        double totalDebits = 0.0;
        long creditCount = 0L;
        long debitCount = 0L;

        for (TransactionResponse t : txs) {
            double amt = t.getAmount() == null ? 0.0 : t.getAmount();
            if ("CREDIT".equalsIgnoreCase(t.getType())) {
                totalCredits += amt;
                creditCount++;
            } else if ("DEBIT".equalsIgnoreCase(t.getType())) {
                totalDebits += amt;
                debitCount++;
            }
        }

        double netChange = totalCredits - totalDebits;

        AccountAnalyticsResponse resp = new AccountAnalyticsResponse();
        resp.setAccountId(accountId);
        resp.setTotalCredits(totalCredits);
        resp.setTotalDebits(totalDebits);
        resp.setNetChange(netChange);
        resp.setCreditCount(creditCount);
        resp.setDebitCount(debitCount);

        return resp;
    }

}
