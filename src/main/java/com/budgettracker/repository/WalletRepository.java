package com.budgettracker.repository;

import com.budgettracker.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByUserIdAndIsDefaultTrue(Long userId);
    boolean existsByUserIdAndName(Long userId, String name);
}
