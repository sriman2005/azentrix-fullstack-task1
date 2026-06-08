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
@RequestMapping("/api/wallets")
public class WalletController {
    @Autowired private WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WalletDto.Response>>> getAll(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getUserWallets(p.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WalletDto.Response>> create(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody WalletDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Wallet created", walletService.createWallet(p.getId(), req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WalletDto.Response>> update(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id, @Valid @RequestBody WalletDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Wallet updated", walletService.updateWallet(id, p.getId(), req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        walletService.deleteWallet(id, p.getId());
        return ResponseEntity.ok(ApiResponse.success("Wallet deleted", null));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Void>> transfer(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody WalletDto.TransferRequest req) {
        walletService.transfer(p.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Transfer successful", null));
    }
}
