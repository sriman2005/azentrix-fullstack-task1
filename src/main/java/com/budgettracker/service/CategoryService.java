package com.budgettracker.service;

import com.budgettracker.dto.CategoryDto;
import com.budgettracker.model.Category;
import com.budgettracker.model.User;
import com.budgettracker.repository.CategoryRepository;
import com.budgettracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    public List<CategoryDto.Response> getUserCategories(Long userId) {
        return categoryRepository.findByUserIdOrIsDefaultTrue(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<CategoryDto.Response> getUserCategoriesByType(Long userId, String type) {
        return categoryRepository.findByUserIdAndType(userId, type)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CategoryDto.Response createCategory(Long userId, CategoryDto.Request request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = Category.builder()
                .user(user)
                .name(request.getName())
                .type(request.getType())
                .icon(request.getIcon())
                .color(request.getColor())
                .isDefault(false)
                .build();

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDto.Response updateCategory(Long categoryId, Long userId, CategoryDto.Request request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (category.isDefault()) throw new RuntimeException("Cannot modify default categories");
        if (!category.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");

        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (category.isDefault()) throw new RuntimeException("Cannot delete default categories");
        if (!category.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");
        categoryRepository.delete(category);
    }

    public CategoryDto.Response toResponse(Category c) {
        CategoryDto.Response r = new CategoryDto.Response();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setType(c.getType());
        r.setIcon(c.getIcon());
        r.setColor(c.getColor());
        r.setDefault(c.isDefault());
        return r;
    }
}
