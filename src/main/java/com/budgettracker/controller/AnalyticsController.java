package com.budgettracker.controller;

import com.budgettracker.dto.ApiResponse;
import com.budgettracker.security.UserPrincipal;
import com.budgettracker.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired private AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDashboardSummary(p.getId())));
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategoryBreakdown(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getCategoryBreakdown(p.getId(), year, month)));
    }

    @GetMapping("/monthly-comparison")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlyComparison(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getMonthlyComparison(p.getId(), year)));
    }

    @GetMapping("/health-score")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthScore(
            @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getFinancialHealthScore(p.getId())));
    }

    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getInsights(
            @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getSmartInsights(p.getId())));
    }
}
