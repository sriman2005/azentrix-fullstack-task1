package com.budgettracker.service;

import com.budgettracker.dto.WalletDto;
import com.budgettracker.model.User;
import com.budgettracker.model.Wallet;
import com.budgettracker.repository.UserRepository;
import com.budgettracker.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    @Autowired private WalletRepository walletRepository;
    @Autowired private UserRepository userRepository;

    public List<WalletDto.Response> getUserWallets(Long userId) {
        return walletRepository.findByUserId(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public WalletDto.Response createWallet(Long userId, WalletDto.Request request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (walletRepository.existsByUserIdAndNameIgnoreCase(userId, request.getName())) {
            throw new RuntimeException("A wallet with name '" + request.getName() + "' already exists");
        }

        if (request.isDefault()) {
            walletRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(w -> {
                w.setDefault(false);
                walletRepository.save(w);
            });
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .name(request.getName())
                .type(request.getType())
                .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
                .currency(request.getCurrency())
                .color(request.getColor())
                .icon(request.getIcon())
                .isDefault(request.isDefault())
                .build();

        return toResponse(walletRepository.save(wallet));
    }

    @Transactional
    public WalletDto.Response updateWallet(Long walletId, Long userId, WalletDto.Request request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (!wallet.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");

        if (request.isDefault() && !wallet.isDefault()) {
            walletRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(w -> {
                w.setDefault(false);
                walletRepository.save(w);
            });
        }

        wallet.setName(request.getName());
        wallet.setType(request.getType());
        wallet.setColor(request.getColor());
        wallet.setIcon(request.getIcon());
        wallet.setDefault(request.isDefault());
        return toResponse(walletRepository.save(wallet));
    }

    @Transactional
    public void deleteWallet(Long walletId, Long userId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (!wallet.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");
        if (wallet.isDefault()) throw new RuntimeException("Cannot delete the default wallet");
        walletRepository.delete(wallet);
    }

    @Transactional
    public void updateBalance(Long walletId, BigDecimal amount, boolean isAddition) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (isAddition) {
            wallet.setBalance(wallet.getBalance().add(amount));
        } else {
            wallet.setBalance(wallet.getBalance().subtract(amount));
        }
        walletRepository.save(wallet);
    }

    @Transactional
    public void transfer(Long userId, WalletDto.TransferRequest req) {
        Wallet from = walletRepository.findById(req.getFromWalletId())
                .orElseThrow(() -> new RuntimeException("Source wallet not found"));
        Wallet to = walletRepository.findById(req.getToWalletId())
                .orElseThrow(() -> new RuntimeException("Destination wallet not found"));

        if (!from.getUser().getId().equals(userId) || !to.getUser().getId().equals(userId))
            throw new RuntimeException("Access denied");
        if (from.getBalance().compareTo(req.getAmount()) < 0)
            throw new RuntimeException("Insufficient balance in source wallet");

        from.setBalance(from.getBalance().subtract(req.getAmount()));
        to.setBalance(to.getBalance().add(req.getAmount()));
        walletRepository.save(from);
        walletRepository.save(to);
    }

    private WalletDto.Response toResponse(Wallet w) {
        WalletDto.Response r = new WalletDto.Response();
        r.setId(w.getId());
        r.setName(w.getName());
        r.setType(w.getType());
        r.setBalance(w.getBalance());
        r.setCurrency(w.getCurrency());
        r.setColor(w.getColor());
        r.setIcon(w.getIcon());
        r.setDefault(w.isDefault());
        r.setCreatedAt(w.getCreatedAt() != null ? w.getCreatedAt().toString() : null);
        return r;
    }
}
