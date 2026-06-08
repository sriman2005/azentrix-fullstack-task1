package com.budgettracker.controller;

import com.budgettracker.dto.*;
import com.budgettracker.security.UserPrincipal;
import com.budgettracker.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {
    @Autowired private SavingsGoalService savingsGoalService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavingsGoalDto.Response>>> getAll(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.success(savingsGoalService.getUserGoals(p.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavingsGoalDto.Response>> create(
            @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody SavingsGoalDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Goal created", savingsGoalService.createGoal(p.getId(), req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsGoalDto.Response>> update(
            @AuthenticationPrincipal UserPrincipal p, @PathVariable Long id,
            @Valid @RequestBody SavingsGoalDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Goal updated", savingsGoalService.updateGoal(id, p.getId(), req)));
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<ApiResponse<SavingsGoalDto.Response>> contribute(
            @AuthenticationPrincipal UserPrincipal p, @PathVariable Long id,
            @Valid @RequestBody SavingsGoalDto.ContributeRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Contribution added", savingsGoalService.contribute(id, p.getId(), req.getAmount())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        savingsGoalService.deleteGoal(id, p.getId());
        return ResponseEntity.ok(ApiResponse.success("Goal deleted", null));
    }
}
