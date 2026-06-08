package com.budgettracker.repository;

import com.budgettracker.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId, Pageable pageable);

    List<Transaction> findByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:walletId IS NULL OR t.wallet.id = :walletId) " +
           "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
           "AND (:minAmount IS NULL OR t.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR t.amount <= :maxAmount) " +
           "AND (:search IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.transactionDate DESC, t.createdAt DESC")
    Page<Transaction> findWithFilters(@Param("userId") Long userId,
                                      @Param("type") String type,
                                      @Param("categoryId") Long categoryId,
                                      @Param("walletId") Long walletId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("minAmount") BigDecimal minAmount,
                                      @Param("maxAmount") BigDecimal maxAmount,
                                      @Param("search") String search,
                                      Pageable pageable);

    List<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndMonthYear(@Param("userId") Long userId,
                                               @Param("year") int year,
                                               @Param("month") int month);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type " +
           "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month")
    BigDecimal sumByUserAndTypeAndMonthYear(@Param("userId") Long userId,
                                            @Param("type") String type,
                                            @Param("year") int year,
                                            @Param("month") int month);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type " +
           "AND t.category.id = :categoryId " +
           "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month")
    BigDecimal sumByCategoryAndMonthYear(@Param("userId") Long userId,
                                          @Param("type") String type,
                                          @Param("categoryId") Long categoryId,
                                          @Param("year") int year,
                                          @Param("month") int month);

    @Query("SELECT t.category.name as category, COALESCE(SUM(t.amount), 0) as total " +
           "FROM Transaction t WHERE t.user.id = :userId AND t.type = 'EXPENSE' " +
           "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month " +
           "GROUP BY t.category.id, t.category.name ORDER BY total DESC")
    List<Object[]> getCategoryBreakdown(@Param("userId") Long userId,
                                         @Param("year") int year,
                                         @Param("month") int month);

    @Query("SELECT MONTH(t.transactionDate) as month, t.type as type, COALESCE(SUM(t.amount), 0) as total " +
           "FROM Transaction t WHERE t.user.id = :userId AND YEAR(t.transactionDate) = :year " +
           "GROUP BY MONTH(t.transactionDate), t.type ORDER BY MONTH(t.transactionDate)")
    List<Object[]> getMonthlyTotals(@Param("userId") Long userId, @Param("year") int year);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.category.id = :categoryId " +
           "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month")
    List<Transaction> findByUserCategoryAndMonthYear(@Param("userId") Long userId,
                                                      @Param("categoryId") Long categoryId,
                                                      @Param("year") int year,
                                                      @Param("month") int month);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.transactionDate = :date ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
