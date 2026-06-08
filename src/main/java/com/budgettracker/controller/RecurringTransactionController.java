package com.budgettracker.controller;

import com.budgettracker.dto.*;
import com.budgettracker.security.UserPrincipal;
import com.budgettracker.service.RecurringTransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring")
public class RecurringTransactionController {
    @Autowired private RecurringTransactionService recurringService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurringTransactionDto.Response>>> getAll(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.success(recurringService.getUserRecurring(p.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringTransactionDto.Response>> create(
            @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody RecurringTransactionDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction created", recurringService.create(p.getId(), req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringTransactionDto.Response>> update(
            @AuthenticationPrincipal UserPrincipal p, @PathVariable Long id,
            @Valid @RequestBody RecurringTransactionDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Updated", recurringService.update(id, p.getId(), req)));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggle(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        recurringService.toggleActive(id, p.getId());
        return ResponseEntity.ok(ApiResponse.success("Toggled", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        recurringService.delete(id, p.getId());
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
