package com.bootcamp.powerhour.config;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.Role;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            if (userRepository.findByUsername("user").isEmpty()) {
                userRepository.save(createUser("user", "user@powerhour.lv", encoder.encode("user"), Role.USER));
                log.info("Seeded default user 'user'");
            }
            if (userRepository.findByUsername("admin").isEmpty()) {
                userRepository.save(createUser("admin", "admin@powerhour.lv", encoder.encode("admin"), Role.ADMIN));
                log.info("Seeded default admin 'admin'");
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
