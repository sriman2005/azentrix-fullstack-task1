package com.budgettracker.service;

import com.budgettracker.dto.AuthDto;
import com.budgettracker.dto.JwtResponse;
import com.budgettracker.model.Category;
import com.budgettracker.model.User;
import com.budgettracker.model.Wallet;
import com.budgettracker.repository.CategoryRepository;
import com.budgettracker.repository.UserRepository;
import com.budgettracker.repository.WalletRepository;
import com.budgettracker.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private EmailService emailService;

    @Value("${app.password-reset.expiry}")
    private long resetTokenExpiry;

    @Transactional
    public JwtResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .currency("INR")
                .darkMode(false)
                .build();

        user = userRepository.save(user);
        seedDefaultCategories(user);
        createDefaultWallet(user);
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        return new JwtResponse(jwt, user.getId(), user.getEmail(),
                user.getFullName(), user.getCurrency(), user.isDarkMode(), user.getRole());
    }

    public JwtResponse login(AuthDto.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new JwtResponse(jwt, user.getId(), user.getEmail(),
                user.getFullName(), user.getCurrency(), user.isDarkMode(), user.getRole());
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plus(java.time.Duration.ofMillis(resetTokenExpiry)));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
    }

    @Transactional
    public void resetPassword(AuthDto.ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, AuthDto.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private void createDefaultWallet(User user) {
        Wallet wallet = Wallet.builder()
                .user(user)
                .name("Main Wallet")
                .type("CASH")
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .color("#6366f1")
                .icon("wallet")
                .isDefault(true)
                .build();
        walletRepository.save(wallet);
    }

    private void seedDefaultCategories(User user) {
        List<Object[]> defaults = List.of(
            // type, name, icon, color
            new Object[]{"EXPENSE","Food & Dining","utensils","#ef4444"},
            new Object[]{"EXPENSE","Transportation","car","#f97316"},
            new Object[]{"EXPENSE","Shopping","shopping-bag","#eab308"},
            new Object[]{"EXPENSE","Entertainment","film","#8b5cf6"},
            new Object[]{"EXPENSE","Healthcare","heart-pulse","#ec4899"},
            new Object[]{"EXPENSE","Education","book-open","#06b6d4"},
            new Object[]{"EXPENSE","Utilities","bolt","#f59e0b"},
            new Object[]{"EXPENSE","Rent & Housing","home","#64748b"},
            new Object[]{"EXPENSE","Personal Care","sparkles","#a855f7"},
            new Object[]{"EXPENSE","Travel","plane","#3b82f6"},
            new Object[]{"EXPENSE","Subscriptions","repeat","#10b981"},
            new Object[]{"EXPENSE","Other","ellipsis","#6b7280"},
            new Object[]{"INCOME","Salary","briefcase","#10b981"},
            new Object[]{"INCOME","Freelance","laptop","#06b6d4"},
            new Object[]{"INCOME","Business","chart-line","#6366f1"},
            new Object[]{"INCOME","Investment","trending-up","#f59e0b"},
            new Object[]{"INCOME","Gift","gift","#ec4899"},
            new Object[]{"INCOME","Other Income","plus-circle","#22c55e"}
        );

        for (Object[] d : defaults) {
            Category cat = Category.builder()
                    .user(user)
                    .type((String) d[0])
                    .name((String) d[1])
                    .icon((String) d[2])
                    .color((String) d[3])
                    .isDefault(false)
                    .build();
            categoryRepository.save(cat);
        }
    }
}
