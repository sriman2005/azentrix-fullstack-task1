package com.budgettracker.service;

import com.budgettracker.dto.BudgetDto;
import com.budgettracker.model.*;
import com.budgettracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    @Autowired private BudgetRepository budgetRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private EmailService emailService;

    public List<BudgetDto.Response> getUserBudgets(Long userId) {
        return budgetRepository.findByUserId(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<BudgetDto.Response> getBudgetsByMonth(Long userId, int year, int month) {
        return budgetRepository.findByUserIdAndMonthAndYear(userId, month, year)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public BudgetDto.Response createBudget(Long userId, BudgetDto.Request request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                userId, request.getCategoryId(), request.getMonth(), request.getYear()).isPresent()) {
            throw new RuntimeException("A budget already exists for this category and period");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Calculate already spent
        BigDecimal spent = transactionRepository.sumByCategoryAndMonthYear(
                userId, "EXPENSE", category.getId(), request.getYear(), request.getMonth());

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .limitAmount(request.getLimitAmount())
                .spentAmount(spent != null ? spent : BigDecimal.ZERO)
                .period(request.getPeriod())
                .month(request.getMonth())
                .year(request.getYear())
                .alertThreshold(request.getAlertThreshold())
                .alertSent(false)
                .build();

        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetDto.Response updateBudget(Long budgetId, Long userId, BudgetDto.Request request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        if (!budget.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");

        budget.setLimitAmount(request.getLimitAmount());
        budget.setAlertThreshold(request.getAlertThreshold());
        budget.setAlertSent(false);
        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(Long budgetId, Long userId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        if (!budget.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");
        budgetRepository.delete(budget);
    }

    @Transactional
    public void checkAndSendAlerts(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        LocalDate now = LocalDate.now();
        budgetRepository.findByUserIdAndMonthAndYear(userId, now.getMonthValue(), now.getYear())
                .forEach(budget -> {
                    if (!budget.isAlertSent() && budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0) {
                        double pct = budget.getSpentAmount()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP)
                                .doubleValue();

                        if (pct >= budget.getAlertThreshold().doubleValue()) {
                            emailService.sendBudgetAlertEmail(
                                    user.getEmail(), user.getFullName(),
                                    budget.getCategory().getName(), pct,
                                    budget.getLimitAmount().doubleValue());
                            budget.setAlertSent(true);
                            budgetRepository.save(budget);
                        }
                    }
                });
    }

    public BudgetDto.Response toResponse(Budget b) {
        BudgetDto.Response r = new BudgetDto.Response();
        r.setId(b.getId());
        r.setCategoryId(b.getCategory().getId());
        r.setCategoryName(b.getCategory().getName());
        r.setCategoryIcon(b.getCategory().getIcon());
        r.setCategoryColor(b.getCategory().getColor());
        r.setLimitAmount(b.getLimitAmount());
        r.setSpentAmount(b.getSpentAmount());
        r.setRemainingAmount(b.getLimitAmount().subtract(b.getSpentAmount()));
        r.setPeriod(b.getPeriod());
        r.setMonth(b.getMonth());
        r.setYear(b.getYear());
        r.setAlertThreshold(b.getAlertThreshold());
        r.setAlertSent(b.isAlertSent());

        if (b.getLimitAmount().compareTo(BigDecimal.ZERO) > 0) {
            double pct = b.getSpentAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(b.getLimitAmount(), 2, RoundingMode.HALF_UP)
                    .doubleValue();
            r.setPercentageUsed(pct);
            r.setStatus(pct >= 100 ? "EXCEEDED" : pct >= b.getAlertThreshold().doubleValue() ? "WARNING" : "SAFE");
        } else {
            r.setPercentageUsed(0);
            r.setStatus("SAFE");
        }
        return r;
    }
}
