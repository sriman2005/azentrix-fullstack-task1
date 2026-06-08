package com.budgettracker.controller;

import com.budgettracker.dto.*;
import com.budgettracker.security.UserPrincipal;
import com.budgettracker.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired private TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionDto.Response>>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        TransactionDto.FilterRequest filter = new TransactionDto.FilterRequest();
        filter.setType(type);
        filter.setCategoryId(categoryId);
        filter.setWalletId(walletId);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setSearch(search);
        filter.setPage(page);
        filter.setSize(size);

        Page<TransactionDto.Response> result = transactionService.getTransactions(principal.getId(), filter);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<TransactionDto.Response>>> getAllTransactions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getAllTransactions(principal.getId())));
    }

    @GetMapping("/month")
    public ResponseEntity<ApiResponse<List<TransactionDto.Response>>> getByMonth(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByMonthYear(principal.getId(), year, month)));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<TransactionDto.Response>>> getByDate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByDate(principal.getId(), date)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionDto.Response>> createTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionDto.Request request) {
        TransactionDto.Response response = transactionService.createTransaction(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Transaction created", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDto.Response>> updateTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TransactionDto.Request request) {
        TransactionDto.Response response = transactionService.updateTransaction(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Transaction updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        transactionService.deleteTransaction(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted", null));
    }
}
