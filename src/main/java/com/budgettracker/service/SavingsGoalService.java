package com.budgettracker.service;

import com.budgettracker.dto.SavingsGoalDto;
import com.budgettracker.model.SavingsGoal;
import com.budgettracker.model.User;
import com.budgettracker.repository.SavingsGoalRepository;
import com.budgettracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SavingsGoalService {

    @Autowired private SavingsGoalRepository savingsGoalRepository;
    @Autowired private UserRepository userRepository;

    public List<SavingsGoalDto.Response> getUserGoals(Long userId) {
        return savingsGoalRepository.findByUserId(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public SavingsGoalDto.Response createGoal(Long userId, SavingsGoalDto.Request request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SavingsGoal goal = SavingsGoal.builder()
                .user(user)
                .name(request.getName())
                .targetAmount(request.getTargetAmount())
                .currentAmount(request.getInitialAmount() != null ? request.getInitialAmount() : BigDecimal.ZERO)
                .deadline(request.getDeadline())
                .icon(request.getIcon())
                .color(request.getColor())
                .status("ACTIVE")
                .build();

        return toResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public SavingsGoalDto.Response updateGoal(Long goalId, Long userId, SavingsGoalDto.Request request) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Savings goal not found"));
        if (!goal.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");

        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setDeadline(request.getDeadline());
        goal.setIcon(request.getIcon());
        goal.setColor(request.getColor());
        return toResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public SavingsGoalDto.Response contribute(Long goalId, Long userId, BigDecimal amount) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Savings goal not found"));
        if (!goal.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");

        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus("COMPLETED");
        }
        return toResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public void deleteGoal(Long goalId, Long userId) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Savings goal not found"));
        if (!goal.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");
        savingsGoalRepository.delete(goal);
    }

    public SavingsGoalDto.Response toResponse(SavingsGoal g) {
        SavingsGoalDto.Response r = new SavingsGoalDto.Response();
        r.setId(g.getId());
        r.setName(g.getName());
        r.setTargetAmount(g.getTargetAmount());
        r.setCurrentAmount(g.getCurrentAmount());
        r.setRemainingAmount(g.getTargetAmount().subtract(g.getCurrentAmount()).max(BigDecimal.ZERO));
        r.setDeadline(g.getDeadline());
        r.setIcon(g.getIcon());
        r.setColor(g.getColor());
        r.setStatus(g.getStatus());
        r.setCreatedAt(g.getCreatedAt() != null ? g.getCreatedAt().toString() : null);

        if (g.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            double pct = g.getCurrentAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(g.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    .doubleValue();
            r.setProgressPercentage(Math.min(pct, 100));
        }

        if (g.getDeadline() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), g.getDeadline());
            r.setDaysRemaining(Math.max(days, 0));
        }

        return r;
    }
}
