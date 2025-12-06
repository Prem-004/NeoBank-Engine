package com.neobankengine.service;

import com.neobankengine.dto.AmountRequest;
import com.neobankengine.dto.CreateAccountRequest;
import com.neobankengine.entity.Account;
import com.neobankengine.entity.Transaction;
import com.neobankengine.entity.User;
import com.neobankengine.exception.BadRequestException;
import com.neobankengine.exception.ForbiddenException;
import com.neobankengine.exception.ResourceNotFoundException;
import com.neobankengine.repository.AccountRepository;
import com.neobankengine.repository.TransactionRepository;
import com.neobankengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService; // injected

    @Transactional
    public Account createAccount(String userEmail, CreateAccountRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = new Account();
        account.setUserId(user.getId());
        account.setBalance(request.getInitialDeposit() == null ? 0.0 : request.getInitialDeposit());
        account.setStatus("ACTIVE");
        account.setCreatedAt(LocalDateTime.now());

        Account saved = accountRepository.save(account);

        // If initial deposit > 0, record transaction
        if (saved.getBalance() != null && saved.getBalance() > 0) {
            Transaction t = new Transaction();
            t.setAccountId(saved.getAccountId());
            t.setType("CREDIT");
            t.setAmount(saved.getBalance());
            t.setReferenceText("Initial deposit");
            t.setTimestamp(LocalDateTime.now());
            transactionRepository.save(t);

            // notify user about initial deposit
            String title = "Initial Deposit";
            String msg = String.format("₹%.2f credited to account %d", saved.getBalance(), saved.getAccountId());
            notificationService.createNotification(user.getEmail(), title, msg, "DEPOSIT", null);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Double getBalance(Long accountId, String userEmail) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // 🚫 Day-15: block non-ACTIVE accounts
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new ForbiddenException("Account is not active");
        }

        // check owner
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!account.getUserId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }

        return account.getBalance();
    }

    @Transactional
    public Account deposit(Long accountId, AmountRequest request, String userEmail) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // 🚫 Day-15: block non-ACTIVE accounts
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new ForbiddenException("Account is not active");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!account.getUserId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }

        account.setBalance((account.getBalance() == null ? 0.0 : account.getBalance()) + request.getAmount());
        Account updated = accountRepository.save(account);

        Transaction t = new Transaction();
        t.setAccountId(accountId);
        t.setType("CREDIT");
        t.setAmount(request.getAmount());
        t.setReferenceText("Deposit");
        t.setTimestamp(LocalDateTime.now());
        transactionRepository.save(t);

        // Notification after saving the transaction
        String title = "Deposit Successful";
        String msg = String.format("₹%.2f deposited to account %d", request.getAmount(), accountId);
        notificationService.createNotification(user.getEmail(), title, msg, "DEPOSIT", null);

        return updated;
    }

    @Transactional
    public Account withdraw(Long accountId, AmountRequest request, String userEmail) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // 🚫 Day-15: block non-ACTIVE accounts
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new ForbiddenException("Account is not active");
        }

        if (userEmail == null || userEmail.isBlank()) {
            throw new BadRequestException("User email is required");
        }

        // fetch user by email (findByEmail returns Optional<User>)
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!account.getUserId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }

        double current = account.getBalance() == null ? 0.0 : account.getBalance();
        if (current < request.getAmount()) {
            throw new BadRequestException("Insufficient balance");
        }

        account.setBalance(current - request.getAmount());
        Account updated = accountRepository.save(account);

        Transaction t = new Transaction();
        t.setAccountId(accountId);
        t.setType("DEBIT");
        t.setAmount(request.getAmount());
        t.setReferenceText("Withdraw");
        t.setTimestamp(LocalDateTime.now());
        transactionRepository.save(t);

        // Notification for withdrawal
        String title = "Withdrawal Successful";
        String msg = String.format("₹%.2f withdrawn from account %d", request.getAmount(), accountId);
        notificationService.createNotification(user.getEmail(), title, msg, "WITHDRAW", null);

        return updated;
    }

    // Helper (currently unused, but kept if you want later refactor)
    private java.util.function.Function<String, java.util.Optional<User>> userUserEmailOr(String email) {
        return e -> userRepository.findByEmail(email);
    }

    @Transactional
    public String transfer(String userEmail, Long fromAccountId, Long toAccountId, Double amount, String note) {
        if (amount == null || amount <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }

        if (fromAccountId.equals(toAccountId)) {
            throw new BadRequestException("From and To account must be different");
        }

        Account accountFrom = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found"));

        Account accountTo = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found"));

        // 🚫 Day-15: block non-ACTIVE accounts for sender & receiver
        if (!"ACTIVE".equalsIgnoreCase(accountFrom.getStatus())) {
            throw new ForbiddenException("Sender account is not active");
        }
        if (!"ACTIVE".equalsIgnoreCase(accountTo.getStatus())) {
            throw new ForbiddenException("Receiver account is not active");
        }

        // Verify caller owns the from-account
        User caller = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Caller user not found"));
        if (!accountFrom.getUserId().equals(caller.getId())) {
            throw new ForbiddenException("Access denied - not owner of source account");
        }

        // Verify balances
        double fromBalance = accountFrom.getBalance() == null ? 0.0 : accountFrom.getBalance();
        if (fromBalance < amount) {
            throw new BadRequestException("Insufficient balance");
        }

        // Load sender & receiver users to get emails for notifications
        User sender = userRepository.findById(accountFrom.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        User receiver = userRepository.findById(accountTo.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        String senderEmail = sender.getEmail();
        String receiverEmail = receiver.getEmail();

        // Update balances
        accountFrom.setBalance(fromBalance - amount);
        accountTo.setBalance((accountTo.getBalance() == null ? 0.0 : accountTo.getBalance()) + amount);

        accountRepository.save(accountFrom);
        accountRepository.save(accountTo);

        // Save transactions
        Transaction debit = new Transaction();
        debit.setAccountId(fromAccountId);
        debit.setType("DEBIT");
        debit.setAmount(amount);
        debit.setReferenceText(note == null ? "Transfer to account " + toAccountId : note);
        debit.setTimestamp(LocalDateTime.now());
        transactionRepository.save(debit);

        Transaction credit = new Transaction();
        credit.setAccountId(toAccountId);
        credit.setType("CREDIT");
        credit.setAmount(amount);
        credit.setReferenceText(note == null ? "Transfer from account " + fromAccountId : note);
        credit.setTimestamp(LocalDateTime.now());
        transactionRepository.save(credit);

        // Notifications: sender then receiver
        String titleSender = "Transfer Sent";
        String msgSender = String.format("₹%.2f transferred to account %d", amount, toAccountId);
        notificationService.createNotification(senderEmail, titleSender, msgSender, "TRANSFER", null);

        String titleReceiver = "Transfer Received";
        String msgReceiver = String.format("₹%.2f received from account %d", amount, fromAccountId);
        notificationService.createNotification(receiverEmail, titleReceiver, msgReceiver, "TRANSFER", null);

        return "Transfer Successful";
    }

    @Transactional(readOnly = true)
    public Double getBalanceBefore(Long accountId, LocalDate fromDate) {
        // if no date requested, return current balance
        if (fromDate == null) {
            Account acc = accountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Account not found"));
            return acc.getBalance() == null ? 0.0 : acc.getBalance();
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // fetch all transactions for account
        List<Transaction> txs = transactionRepository.findByAccountId(accountId);

        // cutoff is the start of the 'from' day (opening = balance *before* that day)
        LocalDateTime cutoff = fromDate.atStartOfDay();

        // compute net amount from cutoff (inclusive) to now (CREDIT positive, DEBIT negative)
        double netSinceCutoff = txs.stream()
                .filter(t -> t.getTimestamp() != null && !t.getTimestamp().isBefore(cutoff))
                .mapToDouble(t -> {
                    Double amt = t.getAmount() == null ? 0.0 : t.getAmount();
                    return "CREDIT".equalsIgnoreCase(t.getType()) ? amt : -amt;
                })
                .sum();

        double current = account.getBalance() == null ? 0.0 : account.getBalance();

        // opening balance = current balance - net of transactions that happened on/after cutoff
        double opening = current - netSinceCutoff;
        return opening;
    }
}
