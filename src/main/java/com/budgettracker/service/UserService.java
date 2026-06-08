package com.budgettracker.service;

import com.budgettracker.model.User;
import com.budgettracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public User updateProfile(Long userId, Map<String, String> updates) {
        User user = getUser(userId);
        if (updates.containsKey("fullName")) user.setFullName(updates.get("fullName"));
        if (updates.containsKey("phone")) user.setPhone(updates.get("phone"));
        if (updates.containsKey("currency")) user.setCurrency(updates.get("currency"));
        return userRepository.save(user);
    }

    @Transactional
    public User updatePreferences(Long userId, boolean darkMode, String currency) {
        User user = getUser(userId);
        user.setDarkMode(darkMode);
        if (currency != null && !currency.isBlank()) user.setCurrency(currency);
        return userRepository.save(user);
    }

    @Transactional
    public User updateAvatar(Long userId, String avatarUrl) {
        User user = getUser(userId);
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }
}
