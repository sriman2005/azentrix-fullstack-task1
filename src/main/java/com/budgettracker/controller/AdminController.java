package com.budgettracker.controller;

import com.budgettracker.dto.ApiResponse;
import com.budgettracker.model.*;
import com.budgettracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private SavingsGoalRepository savingsGoalRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalTransactions", transactionRepository.count());
        stats.put("totalWallets", walletRepository.count());
        stats.put("totalCategories", categoryRepository.count());
        stats.put("totalBudgets", budgetRepository.count());
        stats.put("totalSavingsGoals", savingsGoalRepository.count());
        return ResponseEntity.ok(ApiResponse.success("Admin stats", stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("fullName", u.getFullName());
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone());
            m.put("role", u.getRole());
            m.put("currency", u.getCurrency());
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
            // Count transactions
            long txCount = transactionRepository.findByUserIdOrderByTransactionDateDescCreatedAtDesc(u.getId()).size();
            m.put("transactionCount", txCount);
            // Sum wallets
            BigDecimal totalBalance = walletRepository.findByUserId(u.getId()).stream()
                    .map(Wallet::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
            m.put("totalBalance", totalBalance);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("All users", users));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String newRole = body.get("role");
        if (!"USER".equals(newRole) && !"ADMIN".equals(newRole)) {
            throw new RuntimeException("Invalid role. Must be USER or ADMIN");
        }
        user.setRole(newRole);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User role updated", null));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if ("ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Cannot delete an admin user");
        }
        userRepository.delete(user);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }
}
