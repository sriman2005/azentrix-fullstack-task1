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
@RequestMapping("/api/categories")
public class CategoryController {
    @Autowired private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getAll(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getUserCategories(p.getId())));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getByType(@AuthenticationPrincipal UserPrincipal p, @PathVariable String type) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getUserCategoriesByType(p.getId(), type.toUpperCase())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDto.Response>> create(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody CategoryDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Category created", categoryService.createCategory(p.getId(), req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> update(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id, @Valid @RequestBody CategoryDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Category updated", categoryService.updateCategory(id, p.getId(), req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        categoryService.deleteCategory(id, p.getId());
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }
}
