package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.exception.NotFoundException;
import com.bootcamp.powerhour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository userRepository;

    public List<User> allUsers() {
        return userRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional
    public void deactivate(long userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
        if (user.getUsername().equals(adminUsername)) {
            throw new IllegalArgumentException("You can't deactivate yourself");
        }
        user.setActive(false);
        log.info("User deactivated: username={} by={}", user.getUsername(), adminUsername);
    }
}