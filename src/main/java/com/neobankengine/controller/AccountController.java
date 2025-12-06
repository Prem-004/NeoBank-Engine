package com.neobankengine.controller;

import com.neobankengine.dto.*;
import com.neobankengine.entity.Account;
import com.neobankengine.service.AccountService;
import com.neobankengine.service.PdfService;
import com.neobankengine.service.TransactionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
            @RequestParam(value = "limit", defaultValue = "5") int limit)
    {

        String email = currentUserEmail();
        List<TransactionResponse> list = transactionService.getLastNTransactions(id, email, limit);
        return ResponseEntity.ok(list);
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

    @GetMapping("/{id}/statement")
    public ResponseEntity<StreamingResponseBody> downloadStatement(
            @PathVariable("id") Long id,
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestParam(value = "format", defaultValue = "csv") String format,
            HttpServletResponse response)
    {

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

        // permission check happens inside service (or do here if you prefer)
        List<TransactionResponse> txs = transactionService.getTransactionsForStatement(id, email, from, to);

        if ("json".equalsIgnoreCase(format)) {
            // return JSON list normally
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=statement-account-" + id + ".json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(outputStream -> {
                        // simple JSON stream
                        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(txs);
                        outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                    });
        }

        // CSV streaming
        String filename = "statement-account-" + id + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.setContentType("text/csv");
        StreamingResponseBody stream = out -> {
            // Write header row
            String header = "transactionId,accountId,type,amount,timestamp,referenceText\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));

            // write CSV rows (escape commas/newlines in referenceText)
            for (TransactionResponse t : txs) {
                String ref = t.getReferenceText() == null ? "" : t.getReferenceText().replace("\"", "\"\"");
                String row = String.format("%d,%d,%s,%.2f,%s,\"%s\"\n",
                        t.getTransactionId(),
                        t.getAccountId(),
                        t.getType(),
                        t.getAmount() == null ? 0.0 : t.getAmount(),
                        t.getTimestamp().toString(),
                        ref);
                out.write(row.getBytes(StandardCharsets.UTF_8));
            }
            out.flush();
        };

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }

    @GetMapping("/{id}/statement.pdf")
    public ResponseEntity<StreamingResponseBody> downloadPdfStatement(
            @PathVariable("id") Long id,
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            HttpServletResponse response) {

        String email = currentUserEmail();

        // parse dates (optional) - reuse your existing parsing
        java.time.LocalDate from = null;
        java.time.LocalDate to = null;
        try {
            if (fromStr != null) from = java.time.LocalDate.parse(fromStr);
            if (toStr != null) to = java.time.LocalDate.parse(toStr);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }

        List<TransactionResponse> txs =
                transactionService.getTransactionsForStatement(id, email, from, to);

        Double openingBalance = accountService.getBalanceBefore(id, from);

        byte[] pdfBytes = pdfService.buildStatementPdf(id, txs, openingBalance);


        String filename = "statement-account-" + id + ".pdf";
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);

        StreamingResponseBody stream = out -> {
            out.write(pdfBytes);
            out.flush();
        };

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(stream);
    }

}
