package com.budgettracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

public class BudgetDto {

    @Data
    public static class Request {
        @NotNull(message = "Category is required")
        private Long categoryId;

        @NotNull(message = "Limit amount is required")
        @Positive(message = "Limit amount must be positive")
        private BigDecimal limitAmount;

        @NotBlank(message = "Period is required")
        private String period; // MONTHLY, WEEKLY, YEARLY

        @NotNull(message = "Month is required")
        private Integer month;

        @NotNull(message = "Year is required")
        private Integer year;

        private BigDecimal alertThreshold = new BigDecimal("80");
    }

    @Data
    public static class Response {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private BigDecimal limitAmount;
        private BigDecimal spentAmount;
        private BigDecimal remainingAmount;
        private double percentageUsed;
        private String period;
        private Integer month;
        private Integer year;
        private BigDecimal alertThreshold;
        private boolean alertSent;
        private String status; // SAFE, WARNING, EXCEEDED
    }
}
