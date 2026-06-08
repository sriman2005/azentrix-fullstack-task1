package com.budgettracker.service;

import com.budgettracker.dto.TransactionDto;
import com.budgettracker.model.*;
import com.budgettracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private BudgetRepository budgetRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<TransactionDto.Response> getTransactions(Long userId, TransactionDto.FilterRequest filter) {
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());
        Page<Transaction> page = transactionRepository.findWithFilters(
                userId, filter.getType(), filter.getCategoryId(), filter.getWalletId(),
                filter.getStartDate(), filter.getEndDate(),
                filter.getMinAmount(), filter.getMaxAmount(),
                filter.getSearch(), pageable);
        return page.map(this::toResponse);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<TransactionDto.Response> getAllTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDescCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<TransactionDto.Response> getByDateRange(Long userId, LocalDate start, LocalDate end) {
        return transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<TransactionDto.Response> getByDate(Long userId, LocalDate date) {
        return transactionRepository.findByUserIdAndDate(userId, date)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<TransactionDto.Response> getByMonthYear(Long userId, int year, int month) {
        return transactionRepository.findByUserIdAndMonthYear(userId, year, month)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public TransactionDto.Response createTransaction(Long userId, TransactionDto.Request request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        Wallet wallet = null;
        if (request.getWalletId() != null) {
            wallet = walletRepository.findById(request.getWalletId()).orElse(null);
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .category(category)
                .wallet(wallet)
                .notes(request.getNotes())
                .isRecurring(request.isRecurring())
                .build();

        transaction = transactionRepository.save(transaction);

        // Update wallet balance
        if (wallet != null) {
            boolean isAddition = "INCOME".equals(request.getType());
            wallet.setBalance(isAddition
                    ? wallet.getBalance().add(request.getAmount())
                    : wallet.getBalance().subtract(request.getAmount()));
            walletRepository.save(wallet);
        }

        // Update budget spending
        updateBudgetSpending(userId, category, request.getAmount(), request.getType(),
                request.getTransactionDate());

        return toResponse(transaction);
    }

    @Transactional
    public TransactionDto.Response updateTransaction(Long txId, Long userId, TransactionDto.Request request) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!tx.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");

        // Revert old wallet balance
        if (tx.getWallet() != null) {
            Wallet w = tx.getWallet();
            boolean wasIncome = "INCOME".equals(tx.getType());
            w.setBalance(wasIncome
                    ? w.getBalance().subtract(tx.getAmount())
                    : w.getBalance().add(tx.getAmount()));
            walletRepository.save(w);
        }

        Category category = request.getCategoryId() != null
                ? categoryRepository.findById(request.getCategoryId()).orElse(null) : null;
        Wallet wallet = request.getWalletId() != null
                ? walletRepository.findById(request.getWalletId()).orElse(null) : null;

        tx.setType(request.getType());
        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription());
        tx.setTransactionDate(request.getTransactionDate());
        tx.setCategory(category);
        tx.setWallet(wallet);
        tx.setNotes(request.getNotes());
        tx.setRecurring(request.isRecurring());

        // Apply new wallet balance
        if (wallet != null) {
            boolean isAddition = "INCOME".equals(request.getType());
            wallet.setBalance(isAddition
                    ? wallet.getBalance().add(request.getAmount())
                    : wallet.getBalance().subtract(request.getAmount()));
            walletRepository.save(wallet);
        }

        return toResponse(transactionRepository.save(tx));
    }

    @Transactional
    public void deleteTransaction(Long txId, Long userId) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!tx.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");

        // Revert wallet balance
        if (tx.getWallet() != null) {
            Wallet w = tx.getWallet();
            boolean wasIncome = "INCOME".equals(tx.getType());
            w.setBalance(wasIncome
                    ? w.getBalance().subtract(tx.getAmount())
                    : w.getBalance().add(tx.getAmount()));
            walletRepository.save(w);
        }

        transactionRepository.delete(tx);
    }

    @Transactional
    public TransactionDto.Response attachReceipt(Long txId, Long userId, String receiptUrl) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!tx.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");
        tx.setReceiptUrl(receiptUrl);
        return toResponse(transactionRepository.save(tx));
    }

    private void updateBudgetSpending(Long userId, Category category, BigDecimal amount,
                                       String type, LocalDate date) {
        if (!"EXPENSE".equals(type) || category == null) return;
        int month = date.getMonthValue();
        int year = date.getYear();
        budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(userId, category.getId(), month, year)
                .ifPresent(budget -> {
                    budget.setSpentAmount(budget.getSpentAmount().add(amount));
                    budgetRepository.save(budget);
                });
    }

    public TransactionDto.Response toResponse(Transaction t) {
        TransactionDto.Response r = new TransactionDto.Response();
        r.setId(t.getId());
        r.setType(t.getType());
        r.setAmount(t.getAmount());
        r.setDescription(t.getDescription());
        r.setTransactionDate(t.getTransactionDate());
        r.setNotes(t.getNotes());
        r.setReceiptUrl(t.getReceiptUrl());
        r.setRecurring(t.isRecurring());
        r.setCreatedAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
        if (t.getCategory() != null) {
            r.setCategoryId(t.getCategory().getId());
            r.setCategoryName(t.getCategory().getName());
            r.setCategoryIcon(t.getCategory().getIcon());
            r.setCategoryColor(t.getCategory().getColor());
        }
        if (t.getWallet() != null) {
            r.setWalletId(t.getWallet().getId());
            r.setWalletName(t.getWallet().getName());
        }
        return r;
    }
}
