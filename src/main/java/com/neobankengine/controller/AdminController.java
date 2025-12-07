package com.neobankengine.controller;

import com.neobankengine.entity.Account;
import com.neobankengine.entity.Transaction;
import com.neobankengine.entity.User;
import com.neobankengine.exception.ResourceNotFoundException;
import com.neobankengine.repository.AccountRepository;
import com.neobankengine.repository.TransactionRepository;
import com.neobankengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // ----------------------------------------------------------------
    // USERS
    // ----------------------------------------------------------------

    // Get all users
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Enable / Disable a user (status = true / false)
    // Example:
    //  PUT /api/admin/users/1/status?active=false
    @PutMapping("/users/{id}/status")
    public String updateUserStatus(@PathVariable Long id,
                                   @RequestParam("active") boolean active) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setStatus(active);
        userRepository.save(user);

        return active ? "User activated successfully" : "User deactivated successfully";
    }

    // ----------------------------------------------------------------
    // ACCOUNTS
    // ----------------------------------------------------------------

    // Get all accounts
    @GetMapping("/accounts")
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    // Freeze an account (status = FROZEN)
    // Example: PUT /api/admin/accounts/5/freeze
    @PostMapping("/accounts/{id}/freeze")
    public ResponseEntity<String> freezeAccount(@PathVariable Long id) {
        Account acc = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        acc.setStatus("FROZEN");
        accountRepository.save(acc);
        return ResponseEntity.ok("Account frozen successfully");
    }

    // POST /api/admin/accounts/{id}/unfreeze
    @PostMapping("/accounts/{id}/unfreeze")
    public ResponseEntity<String> unfreezeAccount(@PathVariable Long id) {
        Account acc = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        acc.setStatus("ACTIVE");
        accountRepository.save(acc);
        return ResponseEntity.ok("Account unfrozen successfully");
    }

    // ----------------------------------------------------------------
    // TRANSACTIONS
    // ----------------------------------------------------------------

    // Get all transactions (for audit)
    @GetMapping("/transactions")
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}
