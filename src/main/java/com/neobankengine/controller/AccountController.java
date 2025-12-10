package com.neobankengine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neobankengine.dto.*;
import com.neobankengine.entity.Account;
import com.neobankengine.service.AccountService;
import com.neobankengine.service.PdfService;
import com.neobankengine.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final PdfService pdfService; // injected PdfService

    // Local ObjectMapper configured for Java time (no Spring bean required)
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // helper to get currently authenticated user's email (from JWT subject)
    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody @Valid CreateAccountRequest request) {
        String email = currentUserEmail();
        Account account = accountService.createAccount(email, request);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable("id") Long id) {
        String email = currentUserEmail();
        Double balance = accountService.getBalance(id, email);
        return ResponseEntity.ok(new BalanceResponse(id, balance));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Account> deposit(@PathVariable("id") Long id,
                                           @RequestBody @Valid AmountRequest request) {
        String email = currentUserEmail();
        Account updated = accountService.deposit(id, request, email);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Account> withdraw(@PathVariable("id") Long id,
                                            @RequestBody @Valid AmountRequest request) {
        String email = currentUserEmail();
        Account updated = accountService.withdraw(id, request, email);
        return ResponseEntity.ok(updated);
    }

    /**
     * Paginated transaction history (Page)
     * Example: GET /api/accounts/1/transactions?page=0&size=10
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @PathVariable("id") Long id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        String email = currentUserEmail();
        Page<TransactionResponse> txPage = transactionService.getTransactions(id, email, page, size);
        return ResponseEntity.ok(txPage);
    }

    /**
     * Mini-statement: last N transactions (no pagination object)
     * Example: GET /api/accounts/1/mini-statement?limit=5
     */
    @GetMapping("/{id}/mini-statement")
    public ResponseEntity<List<TransactionResponse>> miniStatement(
            @PathVariable("id") Long id,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {

        String email = currentUserEmail();
        List<TransactionResponse> list = transactionService.getLastNTransactions(id, email, limit);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<AccountAnalyticsResponse> getAnalytics(
            @PathVariable("id") Long id,
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr
    ) {
        String email = currentUserEmail();

        LocalDate from = null;
        LocalDate to = null;
        try {
            if (fromStr != null) from = LocalDate.parse(fromStr);
            if (toStr != null) to = LocalDate.parse(toStr);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }

        AccountAnalyticsResponse resp =
                transactionService.getAnalytics(id, email, from, to);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody @Valid TransferRequest request) {
        String email = currentUserEmail();

        String result = accountService.transfer(
                email,
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getNote()
        );

        return ResponseEntity.ok(result);
    }

    /**
     * CSV / JSON statement download as byte[].
     */
    @GetMapping("/{id}/statement")
    public ResponseEntity<byte[]> downloadStatement(
            @PathVariable("id") Long id,
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestParam(value = "format", defaultValue = "csv") String format
    ) {

        String email = currentUserEmail();

        LocalDate from = null;
        LocalDate to = null;
        try {
            if (fromStr != null) from = LocalDate.parse(fromStr);
            if (toStr != null) to = LocalDate.parse(toStr);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }

        if (!"csv".equalsIgnoreCase(format) && !"json".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().build();
        }

        // permission check happens inside service
        List<TransactionResponse> txs =
                transactionService.getTransactionsForStatement(id, email, from, to);

        HttpHeaders headers = new HttpHeaders();
        byte[] body;

        if ("json".equalsIgnoreCase(format)) {
            // JSON bytes using locally configured ObjectMapper (handles LocalDateTime)
            try {
                body = objectMapper.writeValueAsBytes(txs);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to build JSON statement", ex);
            }

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("statement-account-" + id + ".json")
                            .build()
            );
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(body);
        }

        // CSV branch
        StringBuilder sb = new StringBuilder();
        sb.append("transactionId,accountId,type,amount,timestamp,referenceText\n");

        for (TransactionResponse t : txs) {
            String ref = t.getReferenceText() == null
                    ? ""
                    : t.getReferenceText().replace("\"", "\"\"");
            String ts = (t.getTimestamp() == null)
                    ? ""
                    : t.getTimestamp().toString();

            sb.append(String.format("%d,%d,%s,%.2f,%s,\"%s\"%n",
                    t.getTransactionId(),
                    t.getAccountId(),
                    t.getType(),
                    t.getAmount() == null ? 0.0 : t.getAmount(),
                    ts,
                    ref
            ));
        }

        body = sb.toString().getBytes(StandardCharsets.UTF_8);
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("statement-account-" + id + ".csv")
                        .build()
        );
        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }

    /**
     * PDF statement download as byte[].
     */
    @GetMapping("/{id}/statement.pdf")
    public ResponseEntity<byte[]> downloadPdfStatement(
            @PathVariable("id") Long id,
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr
    ) {
        String email = currentUserEmail();

        LocalDate from = null;
        LocalDate to = null;
        try {
            if (fromStr != null) from = LocalDate.parse(fromStr);
            if (toStr != null) to = LocalDate.parse(toStr);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }

        List<TransactionResponse> txs =
                transactionService.getTransactionsForStatement(id, email, from, to);

        Double openingBalance = accountService.getBalanceBefore(id, from);

        byte[] pdfBytes = pdfService.buildStatementPdf(id, txs, openingBalance);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("statement-account-" + id + ".pdf")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
