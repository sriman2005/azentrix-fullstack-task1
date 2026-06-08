package com.budgettracker.repository;

import com.budgettracker.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrIsDefaultTrue(Long userId);
    List<Category> findByUserIdAndType(Long userId, String type);
    List<Category> findByIsDefaultTrue();
    boolean existsByUserIdAndNameAndType(Long userId, String name, String type);
}
