package com.budgettracker.controller;

import com.budgettracker.dto.*;
import com.budgettracker.security.UserPrincipal;
import com.budgettracker.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    @Autowired private BudgetService budgetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetDto.Response>>> getAll(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getUserBudgets(p.getId())));
    }

    @GetMapping("/month")
    public ResponseEntity<ApiResponse<List<BudgetDto.Response>>> getByMonth(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getBudgetsByMonth(p.getId(), year, month)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetDto.Response>> create(
            @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody BudgetDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Budget created", budgetService.createBudget(p.getId(), req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetDto.Response>> update(
            @AuthenticationPrincipal UserPrincipal p, @PathVariable Long id,
            @Valid @RequestBody BudgetDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Budget updated", budgetService.updateBudget(id, p.getId(), req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        budgetService.deleteBudget(id, p.getId());
        return ResponseEntity.ok(ApiResponse.success("Budget deleted", null));
    }

    @PostMapping("/check-alerts")
    public ResponseEntity<ApiResponse<Void>> checkAlerts(@AuthenticationPrincipal UserPrincipal p) {
        budgetService.checkAndSendAlerts(p.getId());
        return ResponseEntity.ok(ApiResponse.success("Alerts checked", null));
    }
}
