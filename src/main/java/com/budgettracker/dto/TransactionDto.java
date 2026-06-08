package com.budgettracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionDto {

    @Data
    public static class Request {
        @NotBlank(message = "Type is required")
        private String type; // INCOME or EXPENSE

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        private String description;

        @NotNull(message = "Transaction date is required")
        private LocalDate transactionDate;

        private Long categoryId;
        private Long walletId;
        private String notes;
        private boolean recurring;
    }

    @Data
    public static class Response {
        private Long id;
        private String type;
        private BigDecimal amount;
        private String description;
        private LocalDate transactionDate;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private Long categoryId;
        private String walletName;
        private Long walletId;
        private String notes;
        private String receiptUrl;
        private boolean recurring;
        private String createdAt;
    }

    @Data
    public static class FilterRequest {
        private String type;
        private Long categoryId;
        private Long walletId;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String search;
        private int page = 0;
        private int size = 20;
    }
}
