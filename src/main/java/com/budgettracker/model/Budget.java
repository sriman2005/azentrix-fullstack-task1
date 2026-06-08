package com.budgettracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "limit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "spent_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Builder.Default
    private String period = "MONTHLY"; // MONTHLY, WEEKLY, YEARLY

    private Integer month; // 1-12

    private Integer year;

    @Column(name = "alert_threshold")
    @Builder.Default
    private BigDecimal alertThreshold = new BigDecimal("80"); // percentage

    @Column(name = "alert_sent")
    @Builder.Default
    private boolean alertSent = false;
}
