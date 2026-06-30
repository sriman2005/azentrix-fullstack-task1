package com.budgettracker.config;

import com.budgettracker.model.User;
import com.budgettracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@budgettracker.com")) {
            User admin = User.builder()
                    .fullName("Admin")
                    .email("admin@budgettracker.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role("ADMIN")
                    .currency("INR")
                    .darkMode(true)
                    .build();
            userRepository.save(admin);
            log.info("✅ Admin user seeded: admin@budgettracker.com / Admin@123");
        }
    }
}
