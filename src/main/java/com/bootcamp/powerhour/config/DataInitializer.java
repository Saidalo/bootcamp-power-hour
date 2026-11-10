package com.bootcamp.powerhour.config;

import com.bootcamp.powerhour.entity.Role;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
public class DataInitializer {

    @Value("${SEED_ADMIN_USERNAME:}")
    private String adminUsername;

    @Value("${SEED_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${SEED_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${SEED_USER_USERNAME:}")
    private String userUsername;

    @Value("${SEED_USER_EMAIL:}")
    private String userEmail;

    @Value("${SEED_USER_PASSWORD:}")
    private String userPassword;

    @Bean
    public CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            if (adminUsername.isBlank() || adminEmail.isBlank() || adminPassword.isBlank()) {
                log.warn("Seed admin is not configured in .env, skipping");
            } else if (!userRepository.existsByUsernameIgnoreCase(adminUsername)) {
                userRepository.save(createUser(adminUsername, adminEmail, encoder.encode(adminPassword), Role.ADMIN));
                log.info("Seeded admin '{}'", adminUsername);
            }

            if (userUsername.isBlank() || userEmail.isBlank() || userPassword.isBlank()) {
                log.warn("Seed user is not configured in .env, skipping");
            } else if (!userRepository.existsByUsernameIgnoreCase(userUsername)) {
                userRepository.save(createUser(userUsername, userEmail, encoder.encode(userPassword), Role.USER));
                log.info("Seeded user '{}'", userUsername);
            }
        };
    }

    private User createUser(String username, String email, String passwordHash, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
