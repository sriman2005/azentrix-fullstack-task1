package com.budgettracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SavingsGoalDto {

    @Data
    public static class Request {
        @NotBlank(message = "Name is required")
        private String name;

        @NotNull(message = "Target amount is required")
        @Positive(message = "Target amount must be positive")
        private BigDecimal targetAmount;

        private BigDecimal initialAmount;
        private LocalDate deadline;
        private String icon = "piggy-bank";
        private String color = "#10b981";
    }

    @Data
    public static class ContributeRequest {
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private BigDecimal targetAmount;
        private BigDecimal currentAmount;
        private BigDecimal remainingAmount;
        private double progressPercentage;
        private LocalDate deadline;
        private String icon;
        private String color;
        private String status;
        private long daysRemaining;
        private String createdAt;
    }
}
