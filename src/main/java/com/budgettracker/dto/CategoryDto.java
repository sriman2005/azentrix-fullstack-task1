package com.budgettracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class CategoryDto {

    @Data
    public static class Request {
        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Type is required")
        private String type; // INCOME or EXPENSE

        private String icon = "tag";
        private String color = "#6366f1";
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private String type;
        private String icon;
        private String color;
        private boolean isDefault;
    }
}
