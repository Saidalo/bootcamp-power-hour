package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.RegisterRequest;
import com.bootcamp.powerhour.dto.UserResponse;
import com.bootcamp.powerhour.entity.Role;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.exception.UserAlreadyExistsException;
import com.bootcamp.powerhour.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new UserAlreadyExistsException(
                    "username", request.getUsername());
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "email", request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setActive(true);
        user.setBiddingZone(request.getBiddingZone());
        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
