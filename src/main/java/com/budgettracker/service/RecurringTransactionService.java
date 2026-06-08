package com.budgettracker.service;

import com.budgettracker.dto.RecurringTransactionDto;
import com.budgettracker.model.*;
import com.budgettracker.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecurringTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(RecurringTransactionService.class);

    @Autowired private RecurringTransactionRepository recurringRepo;
    @Autowired private TransactionRepository transactionRepo;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionService transactionService;

    public List<RecurringTransactionDto.Response> getUserRecurring(Long userId) {
        return recurringRepo.findByUserId(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public RecurringTransactionDto.Response create(Long userId, RecurringTransactionDto.Request request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = request.getCategoryId() != null
                ? categoryRepository.findById(request.getCategoryId()).orElse(null) : null;
        Wallet wallet = request.getWalletId() != null
                ? walletRepository.findById(request.getWalletId()).orElse(null) : null;

        RecurringTransaction rt = RecurringTransaction.builder()
                .user(user)
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .category(category)
                .wallet(wallet)
                .frequency(request.getFrequency())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .nextExecution(request.getStartDate())
                .isActive(true)
                .build();

        return toResponse(recurringRepo.save(rt));
    }

    @Transactional
    public RecurringTransactionDto.Response update(Long id, Long userId, RecurringTransactionDto.Request request) {
        RecurringTransaction rt = recurringRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
        if (!rt.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");

        rt.setAmount(request.getAmount());
        rt.setDescription(request.getDescription());
        rt.setFrequency(request.getFrequency());
        rt.setEndDate(request.getEndDate());
        return toResponse(recurringRepo.save(rt));
    }

    @Transactional
    public void toggleActive(Long id, Long userId) {
        RecurringTransaction rt = recurringRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
        if (!rt.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");
        rt.setActive(!rt.isActive());
        recurringRepo.save(rt);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        RecurringTransaction rt = recurringRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
        if (!rt.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");
        recurringRepo.delete(rt);
    }

    @Scheduled(cron = "0 0 6 * * *") // Run daily at 6 AM
    @Transactional
    public void processRecurringTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> due = recurringRepo.findByIsActiveTrueAndNextExecutionLessThanEqual(today);

        for (RecurringTransaction rt : due) {
            try {
                if (rt.getEndDate() != null && today.isAfter(rt.getEndDate())) {
                    rt.setActive(false);
                    recurringRepo.save(rt);
                    continue;
                }

                Transaction tx = Transaction.builder()
                        .user(rt.getUser())
                        .type(rt.getType())
                        .amount(rt.getAmount())
                        .description(rt.getDescription() + " (Recurring)")
                        .transactionDate(today)
                        .category(rt.getCategory())
                        .wallet(rt.getWallet())
                        .isRecurring(true)
                        .build();
                transactionRepo.save(tx);

                // Update wallet balance
                if (rt.getWallet() != null) {
                    Wallet w = rt.getWallet();
                    boolean isIncome = "INCOME".equals(rt.getType());
                    w.setBalance(isIncome
                            ? w.getBalance().add(rt.getAmount())
                            : w.getBalance().subtract(rt.getAmount()));
                    walletRepository.save(w);
                }

                // Advance next execution date
                rt.setNextExecution(calculateNextDate(today, rt.getFrequency()));
                recurringRepo.save(rt);

            } catch (Exception e) {
                logger.error("Error processing recurring transaction {}: {}", rt.getId(), e.getMessage());
            }
        }
        logger.info("Processed {} recurring transactions", due.size());
    }

    private LocalDate calculateNextDate(LocalDate current, String frequency) {
        return switch (frequency) {
            case "DAILY" -> current.plusDays(1);
            case "WEEKLY" -> current.plusWeeks(1);
            case "MONTHLY" -> current.plusMonths(1);
            case "YEARLY" -> current.plusYears(1);
            default -> current.plusMonths(1);
        };
    }

    public RecurringTransactionDto.Response toResponse(RecurringTransaction rt) {
        RecurringTransactionDto.Response r = new RecurringTransactionDto.Response();
        r.setId(rt.getId());
        r.setType(rt.getType());
        r.setAmount(rt.getAmount());
        r.setDescription(rt.getDescription());
        r.setFrequency(rt.getFrequency());
        r.setStartDate(rt.getStartDate());
        r.setEndDate(rt.getEndDate());
        r.setNextExecution(rt.getNextExecution());
        r.setActive(rt.isActive());
        if (rt.getCategory() != null) {
            r.setCategoryName(rt.getCategory().getName());
            r.setCategoryIcon(rt.getCategory().getIcon());
            r.setCategoryColor(rt.getCategory().getColor());
        }
        if (rt.getWallet() != null) {
            r.setWalletName(rt.getWallet().getName());
        }
        return r;
    }
}
