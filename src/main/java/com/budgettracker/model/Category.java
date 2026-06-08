package com.budgettracker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private String type = "EXPENSE"; // INCOME or EXPENSE

    @Builder.Default
    private String icon = "tag";

    @Builder.Default
    private String color = "#6366f1";

    @Column(name = "is_default")
    @Builder.Default
    private boolean isDefault = false;
}
