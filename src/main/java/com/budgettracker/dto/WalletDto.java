package com.budgettracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WalletDto {

    @Data
    public static class Request {
        @NotBlank(message = "Wallet name is required")
        private String name;

        private String type = "CASH";
        private BigDecimal balance = BigDecimal.ZERO;
        private String currency = "INR";
        private String color = "#6366f1";
        private String icon = "wallet";
        private boolean isDefault = false;
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private String type;
        private BigDecimal balance;
        private String currency;
        private String color;
        private String icon;
        private boolean isDefault;
        private String createdAt;
    }

    @Data
    public static class TransferRequest {
        @NotNull
        private Long fromWalletId;
        @NotNull
        private Long toWalletId;
        @NotNull
        @Positive
        private BigDecimal amount;
        private String description;
        private LocalDate date;
    }
}
