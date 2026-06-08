package com.budgettracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecurringTransactionDto {

    @Data
    public static class Request {
        @NotBlank(message = "Type is required")
        private String type;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        private String description;
        private Long categoryId;
        private Long walletId;

        @NotBlank(message = "Frequency is required")
        private String frequency; // DAILY, WEEKLY, MONTHLY, YEARLY

        @NotNull(message = "Start date is required")
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class Response {
        private Long id;
        private String type;
        private BigDecimal amount;
        private String description;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private String walletName;
        private String frequency;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate nextExecution;
        private boolean active;
    }
}
