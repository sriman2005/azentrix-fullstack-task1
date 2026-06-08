package com.budgettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String fullName;
    private String currency;
    private boolean darkMode;

    public JwtResponse(String token, Long id, String email, String fullName, String currency, boolean darkMode) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.currency = currency;
        this.darkMode = darkMode;
    }
}
