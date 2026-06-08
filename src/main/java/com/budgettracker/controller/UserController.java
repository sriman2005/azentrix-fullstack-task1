package com.budgettracker.controller;

import com.budgettracker.dto.ApiResponse;
import com.budgettracker.dto.AuthDto;
import com.budgettracker.model.User;
import com.budgettracker.security.UserPrincipal;
import com.budgettracker.service.AuthService;
import com.budgettracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserService userService;
    @Autowired private AuthService authService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userService.getUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(userToMap(user)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> updates) {
        User user = userService.updateProfile(principal.getId(), updates);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userToMap(user)));
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> prefs) {
        boolean darkMode = Boolean.parseBoolean(prefs.getOrDefault("darkMode", false).toString());
        String currency = (String) prefs.getOrDefault("currency", "INR");
        User user = userService.updatePreferences(principal.getId(), darkMode, currency);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", userToMap(user)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("phone", user.getPhone());
        map.put("avatarUrl", user.getAvatarUrl());
        map.put("darkMode", user.isDarkMode());
        map.put("currency", user.getCurrency());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }
}
